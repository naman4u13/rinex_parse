package com.RINEX_parser.fileParser;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.IntStream;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import com.RINEX_parser.utility.StringUtil;
import com.RINEX_parser.utility.Time;
import com.opencsv.CSVWriter;

public class Antenna {

	public static double[] getSunCoord(CelestialBody sun, long GPSTime, long weekNo, long leapSeconds) {

		Date date = Time.getDate(GPSTime + leapSeconds, weekNo, 0).getTime();
		AbsoluteDate absDate = new AbsoluteDate(date, TimeScalesFactory.getUTC());
		Vector3D coords = sun.getPVCoordinates(absDate, FramesFactory.getEME2000()).getPosition();
		return new double[] { coords.getX(), coords.getY(), coords.getZ() };

	}

	public static void buildCSV(String path) throws Exception {
		try {

			Set<Character> SSIset = Set.of('G', 'R', 'E', 'C', 'I', 'S', 'J');

			String outputFilePath = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\antenna2.csv";
			String[] header = { "TYPE", "SVID/SrNo", "DAZI", "ZEN", "VALID_FROM", "VALID_UNTIL", "FREQUENCY", "NEU",
					"PCV_NOAZI", "PCV_AZI" };
			CSVWriter writer = null;

			// create FileWriter object with file as parameter
			FileWriter outputfile = new FileWriter(new File(outputFilePath));
			// create CSVWriter object filewriter object as parameter
			writer = new CSVWriter(outputfile);
			writer.writeNext(header);

			File file = new File(path);
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
				int col = (int) (zen[1] - zen[0] / zen[2]) + 1;

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

					String[] _NEU = StringUtil.splitter(lines[index++], false, 10, 10, 10, 30);
					double[] NEU = IntStream.range(0, 3).mapToDouble(x -> Double.parseDouble(_NEU[x])).toArray();

					String[] NOAZI = lines[index++].trim().split("\\s+");
					double[] PCVnoazi = IntStream.range(1, NOAZI.length).mapToDouble(x -> Double.parseDouble(NOAZI[x]))
							.toArray();
					double[][] PCVazi = null;
					if (dazi > 0) {
						int row = (int) (360 / dazi) + 1;
						PCVazi = new double[row][col];
						for (int j = 0; j < row; j++) {

							PCVazi[j] = Arrays.stream(lines[index++].trim().split("\\s+"))
									.mapToDouble(x -> Double.parseDouble(x.trim())).toArray();

						}
					}
					String[] line = new String[] { type + "", typeCode, dazi + "", Arrays.toString(zen),
							Arrays.toString(validFrom), Arrays.toString(validUntil), freq, Arrays.toString(NEU),
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

}
