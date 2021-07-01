package com.RINEX_parser.fileParser.GoogleDecimeter;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import com.RINEX_parser.utility.StringUtil;
import com.RINEX_parser.utility.Time;
import com.opencsv.CSVReader;

public class GroundTruth {
	private static final long NumberMilliSecondsWeek = 604800000;

	public static void processCSV(String path) throws Exception {
		ArrayList<Double> timeList = new ArrayList<Double>();
		ArrayList<double[]> llhList = new ArrayList<double[]>();
		try {
			CSVReader reader = new CSVReader(new FileReader(path));
			String[] line;
			String[] header = reader.readNext();
			// reads one line at a time
			while ((line = reader.readNext()) != null) {
				double GPStime = ((Double.parseDouble(line[2]) % NumberMilliSecondsWeek) / 1000) % 86400;
				double lat = Double.parseDouble(line[3]);
				double lon = Double.parseDouble(line[4]);
				double alt = Double.parseDouble(line[5]);
				timeList.add(GPStime);
				llhList.add(new double[] { lat, lon, alt });

			}
			reader.close();
			System.out.print("");
		} catch (Exception e) {
			// TODO: handle exception
			throw new Exception("Error occured during parsing of Ground Truth CSV file \n" + e);

		}

	}

	public static void processNMEA(String path) throws Exception {
		Path fileName = Path.of(path);
		String input = Files.readString(fileName);
		String[] lines = input.split("\r\n|\r|\n");
		ArrayList<double[]> timeList = new ArrayList<double[]>();
		ArrayList<double[]> llhList = new ArrayList<double[]>();
		for (int i = 0; i < lines.length; i++) {
			String GGA = lines[i];
			i++;
			String RMC = lines[i];
			String[] ggaParam = GGA.split(",");
			String[] strDate = StringUtil.splitter(RMC.split(",")[9], 2, 2, 2);
			String[] strUTC = StringUtil.splitter(ggaParam[1], false, 2, 2);
			int[] date = Arrays.stream(strDate).mapToInt(j -> Integer.parseInt(j)).toArray();
			int hour = Integer.parseInt(strUTC[0]);
			int min = Integer.parseInt(strUTC[1]);
			double sec = Double.parseDouble(strUTC[2]);
			double[] time = Time.getGPSTime(date[2] + 2000, date[1] - 1, date[0], hour, min, sec);
			double lat = Double.parseDouble(ggaParam[2]);
			String latSign = ggaParam[3];
			if (latSign.equals("S")) {
				lat *= -1;
			}
			double lon = Double.parseDouble(ggaParam[4]);
			String lonSign = ggaParam[5];
			if (lonSign.equals("W")) {
				lon *= -1;
			}
			double alt = Double.parseDouble(ggaParam[9]);
			timeList.add(time);
			llhList.add(new double[] { lat, lon, alt });

		}
		System.out.print("");

	}

}
