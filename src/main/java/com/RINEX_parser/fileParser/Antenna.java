package com.RINEX_parser.fileParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.IntStream;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import com.RINEX_parser.models.IGS.IGSAntenna;
import com.RINEX_parser.utility.StringUtil;
import com.RINEX_parser.utility.Time;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class Antenna {

	private HashMap<Character, HashMap<Integer, HashMap<Integer, ArrayList<IGSAntenna>>>> satAntMap;

	private CelestialBody sun;

	public Antenna(String path) throws Exception {
		satAntMap = new HashMap<Character, HashMap<Integer, HashMap<Integer, ArrayList<IGSAntenna>>>>();
		readCSV(path);
		// Remember it is necessary that buildGeoid in Main fun runs first so that
		// Orekit's DataManagerProvider is intialized before fetching Sun
		sun = CelestialBodyFactory.getSun();

	}

	private void readCSV(String path) throws Exception {
		try {
			// parsing a CSV file into CSVReader class constructor
			CSVReader reader = new CSVReader(new FileReader(path));
			String[] line;
			reader.readNext();
			// reads one line at a time
			while ((line = reader.readNext()) != null) {

				char antType = line[0].charAt(0);
				if (antType != 'S') {
					reader.close();
					return;
				}
				String _SVID = line[1];
				char SSI = _SVID.charAt(0);
				int SVID = Integer.parseInt(_SVID.substring(1));
				String _freq = line[6];
				int freq = Integer.parseInt(_freq.substring(1));
				IGSAntenna satAnt = new IGSAntenna(line[2], line[3], line[4], line[5], line[7], line[8], line[9]);
				satAntMap.computeIfAbsent(SSI, k -> new HashMap<Integer, HashMap<Integer, ArrayList<IGSAntenna>>>())
						.computeIfAbsent(SVID, k -> new HashMap<Integer, ArrayList<IGSAntenna>>())
						.computeIfAbsent(freq, k -> new ArrayList<IGSAntenna>()).add(satAnt);

			}
			reader.close();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			throw new Exception("Error occured during reading and parsing of Antenna(.csv) file \n" + e);
		}

	}

	public double[] getSatPC(int SVID, String obsvCode, long GPSTime, long weekNo, double[] satMC) {

		return getSatPC(SVID, new String[] { obsvCode }, GPSTime, weekNo, satMC)[0];

	}

	public double[][] getSatPC(int SVID, String[] obsvCode, long GPSTime, long weekNo, double[] satMC) {
		int fN = obsvCode.length;
		double[][] eccXYZ = new double[fN][];
		for (int i = 0; i < fN; i++) {

			char SSI = obsvCode[i].charAt(0);
			int freq = Integer.parseInt(obsvCode[i].charAt(1) + "");
			ArrayList<IGSAntenna> satAntList = satAntMap.get(SSI).get(SVID).get(freq);
			int n = satAntList.size();

			for (int j = n - 1; j >= 0; j--) {
				IGSAntenna satAnt = satAntList.get(j);

				if (satAnt.checkValidity(new long[] { GPSTime, weekNo })) {
					eccXYZ[i] = satAnt.getEccXYZ();
					break;
				}

			}
		}

		double R_sat = Math
				.sqrt(IntStream.range(0, 3).mapToDouble(i -> satMC[i] * satMC[i]).reduce(0, (i, j) -> i + j));
		double[] k = IntStream.range(0, 3).mapToDouble(i -> -satMC[i] / R_sat).toArray();
		double[] sunXYZ = getSunCoord(GPSTime, weekNo);
		double[] e = IntStream.range(0, 3).mapToDouble(i -> sunXYZ[i] - satMC[i]).toArray();
		double R_sun_sat = Math.sqrt(IntStream.range(0, 3).mapToDouble(i -> e[i] * e[i]).reduce(0, (i, j) -> i + j));
		IntStream.range(0, 3).forEach(i -> e[i] = e[i] / R_sun_sat);
		double[] j = crossProd(k, e);
		double[] i = crossProd(j, k);

		double[][] satPC = new double[fN][3];
		for (int x = 0; x < fN; x++) {
			for (int y = 0; y < 3; y++) {
				satPC[x][y] = satMC[y] + (eccXYZ[x][0] * i[y]) + (eccXYZ[x][1] * j[y]) + (eccXYZ[x][2] * k[y]);
			}
		}

		return satPC;
	}

	private double[] getSunCoord(long GPSTime, long weekNo) {

		Date date = Time.getDate(GPSTime + 19, weekNo, 0).getTime();
		AbsoluteDate absDate = new AbsoluteDate(date, TimeScalesFactory.getTAI());
		Vector3D coords = sun.getPVCoordinates(absDate, FramesFactory.getEME2000()).getPosition();
		return new double[] { coords.getX(), coords.getY(), coords.getZ() };

	}

	public static void buildCSV(String in_path, String out_path) throws Exception {
		try {

			Set<Character> SSIset = Set.of('G', 'R', 'E', 'C', 'I', 'S', 'J');

			String[] header = { "TYPE", "SVID/SrNo", "DAZI", "ZEN", "VALID_FROM", "VALID_UNTIL", "FREQUENCY", "NEU/XYZ",
					"PCV_NOAZI", "PCV_AZI" };
			CSVWriter writer = null;

			// create FileWriter object with file as parameter
			FileWriter outputfile = new FileWriter(new File(out_path));
			// create CSVWriter object filewriter object as parameter
			writer = new CSVWriter(outputfile);
			writer.writeNext(header);

			File file = new File(in_path);
			Scanner input = new Scanner(file);
			input.useDelimiter("END OF HEADER");
			input.next();
			input.useDelimiter("START OF ANTENNA|END OF ANTENNA");
			input.next();
			while (input.hasNext()) {
				String str = input.next().trim();
				if (str.isBlank()) {
					continue;
				}
				String[] lines = str.split("\n");

				String[] type_sno = StringUtil.splitter(lines[0], false, 20, 20, 10, 10);
				char type = 'R';
				String typeCode = type_sno[1];
				if (typeCode.length() == 3 && SSIset.contains(typeCode.charAt(0))) {
					type = 'S';
				} else {
					typeCode = type_sno[0];
					// break;
				}

				double dazi = Double.parseDouble(StringUtil.splitter(lines[2], false, 2, 6, 52)[1]);
				String[] _zen = StringUtil.splitter(lines[3], false, 2, 6, 6, 6, 40);
				double[] zen = IntStream.range(1, 4).mapToDouble(x -> Double.parseDouble(_zen[x])).toArray();
				int col = (int) ((zen[1] - zen[0]) / zen[2]) + 1;

				int freqCount = Integer.parseInt(StringUtil.splitter(lines[4], false, 6, 54)[0]);

				String[] temp = StringUtil.splitter(lines[5], 60, 20);
				long[] validFrom = null;
				int index = 6;
				if (temp[1].equalsIgnoreCase("VALID FROM")) {

					String[] _validFrom = temp[0].split("\\s+");
					validFrom = Time.getGPSTime(_validFrom);
					index++;
				}

				temp = StringUtil.splitter(lines[6], 60, 20);
				long[] validUntil = null;

				if (temp[1].equalsIgnoreCase("VALID UNTIL")) {

					String[] _validUntil = temp[0].split("\\s+");
					validUntil = Time.getGPSTime(_validUntil);
					index++;
				}
				while (true) {
					temp = StringUtil.splitter(lines[index], 60, 20);
					if (!temp[1].equalsIgnoreCase("COMMENT")) {
						break;
					}
					index++;

				}
				while (freqCount > 0) {
					String freq = StringUtil.splitter(lines[index++], false, 3, 3, 54)[1];

					String[] _NEU_XYZ = StringUtil.splitter(lines[index++], false, 10, 10, 10, 30);
					double[] NEU_XYZ = IntStream.range(0, 3).mapToDouble(x -> Double.parseDouble(_NEU_XYZ[x]))
							.toArray();

					String[] NOAZI = lines[index++].trim().split("\\s+");
					double[] PCVnoazi = IntStream.range(1, NOAZI.length).mapToDouble(x -> Double.parseDouble(NOAZI[x]))
							.toArray();
					double[][] PCVazi = null;
					if (dazi > 0) {
						int row = (int) (360 / dazi) + 1;
						PCVazi = new double[row][col];
						for (int j = 0; j < row; j++) {

							String[] strArr = lines[index++].trim().split("\\s+");

							PCVazi[j] = IntStream.range(1, col + 1)
									.mapToDouble(x -> Double.parseDouble(strArr[x].trim())).toArray();

						}
					}
					String[] line = new String[] { type + "", typeCode, dazi + "", Arrays.toString(zen),
							Arrays.toString(validFrom), Arrays.toString(validUntil), freq, Arrays.toString(NEU_XYZ),
							Arrays.toString(PCVnoazi), Arrays.deepToString(PCVazi) };
					writer.writeNext(line);
					index++;
					freqCount--;

				}

			}
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(
					"Error occured during parsing of Antenna(.atx) file or while generating CSV table \n" + e);

		}

	}

	private double[] crossProd(double[] a, double[] b) {
		double[] c = new double[3];
		c[0] = (a[1] * b[2]) - (a[2] * b[1]);
		c[1] = -((a[0] * b[2]) - (a[2] * b[0]));
		c[2] = (a[0] * b[1]) - (a[1] * b[0]);
		return c;
	}

}
