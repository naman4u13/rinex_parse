package com.RINEX_parser.utility;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.IntStream;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class IGPgrid {

	public static void buildCSV() {

		String[] header = { "Band", "Point", "Lat", "Lon" };
		int[] data3 = { -75, -65, -55, -50, -45, -40, -35, -30, -25, -20, -15, -10, -5, 0, 5, 10, 15, 20, 25, 30, 35,
				40, 45, 50, 55, 65, 75 };
		int[] data4 = { -55, -50, -45, -40, -35, -30, -25, -20, -15, -10, -5, 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50,
				55 };
		int[] data1 = { -75, -65, -55, -50, -45, -40, -35, -30, -25, -20, -15, -10, -5, 0, 5, 10, 15, 20, 25, 30, 35,
				40, 45, 50, 55, 65, 75, 85 };
		int[] data2 = { -85, -75, -65, -55, -50, -45, -40, -35, -30, -25, -20, -15, -10, -5, 0, 5, 10, 15, 20, 25, 30,
				35, 40, 45, 50, 55, 65, 75 };

		String filePath = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\SBAS_IGP\\Grid2.csv";

		try {

			int flag1 = -36;
			int flag2 = -28;
			int flag = -36;
			int band = -1;
			int point = 1;
			CSVWriter writer = null;

			File file = new File(filePath);
			// create FileWriter object with file as parameter
			FileWriter outputfile = new FileWriter(file);
			// create CSVWriter object filewriter object as parameter
			writer = new CSVWriter(outputfile);
			writer.writeNext(header);
			for (int i = -36; i <= 35; i++) {
				if (i == flag) {
					band++;
					point = 1;

					flag += 8;

				}
				int lon = i * 5;
				int[] data = null;
				if (i == flag1) {
					data = data1;
					flag1 += 18;
				} else if (i == flag2) {
					data = data2;
					flag2 += 18;

				} else {
					if (i % 2 == 0) {
						data = data3;

					} else {
						data = data4;
					}
				}
				for (int lat : data) {

					String[] line = new String[] { band + "", point + "", lat + "", lon + "" };
					writer.writeNext(line);
					point++;
				}
			}

			int[] data5 = IntStream.range(-36, 36).map(i -> i * 5).toArray();
			int[] data6 = IntStream.range(-18, 18).map(i -> i * 10).toArray();
			int[] data7 = IntStream.range(-6, 6).map(i -> i * 30).toArray();

			ArrayList<String[]> band9 = new ArrayList<String[]>();
			ArrayList<String[]> band10 = new ArrayList<String[]>();
			point = 1;
			for (int i = 60; i <= 85; i += 5) {
				if (i == 80) {
					continue;
				}
				int[] data = null;
				int lat = i;
				if (i == 60) {
					data = data5;
				} else if (i == 85) {
					data = data7;

				} else {
					data = data6;
				}
				for (int lon : data) {

					band9.add(new String[] { 9 + "", point + "", lat + "", lon + "" });
					if (i == 85) {
						lon = lon + 10;
					}
					band10.add(new String[] { 10 + "", point + "", -lat + "", lon + "" });

					point++;
				}

			}
			writer.writeAll(band9);
			writer.writeAll(band10);
			// closing writer connection
			writer.close();

		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public static int[][][] readCSV() {

		int[] bandSize = new int[] { 201, 201, 201, 201, 201, 201, 201, 201, 200, 192, 192 };
		int bandCount = bandSize.length;// values is 11
		int[][][] IGPgrid = new int[bandCount][][];
		IntStream.range(0, bandCount).forEach(i -> IGPgrid[i] = new int[bandSize[i]][]);

		try {
			// parsing a CSV file into CSVReader class constructor
			CSVReader reader = new CSVReader(
					new FileReader("C:\\Users\\Naman\\Desktop\\rinex_parse_files\\SBAS_IGP\\Grid.csv"));
			String[] nextLine;
			reader.readNext();
			// reads one line at a time
			while ((nextLine = reader.readNext()) != null) {

				int band = Integer.parseInt(nextLine[0]);
				int point = Integer.parseInt(nextLine[1]);
				int lat = Integer.parseInt(nextLine[2]);
				int lon = Integer.parseInt(nextLine[3]);
				IGPgrid[band][point - 1] = new int[] { lat, lon };

			}
		} catch (Exception e) {
			System.out.println(e);
			// TODO: handle exception
		}
		return IGPgrid;
	}

	public static void recordIPPdelay(ArrayList<String[]> IPPdelay) {
		String[] header = { "Lat", "Lon", "GPS TOW", "IPP Zenith Delay(m)" };
		String filePath = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\SBAS_IPP\\2020_100_PRN123.csv";
		CSVWriter writer = null;

		File file = new File(filePath);
		// create FileWriter object with file as parameter
		FileWriter outputfile;
		try {
			outputfile = new FileWriter(file);
			// create CSVWriter object filewriter object as parameter
			writer = new CSVWriter(outputfile);
			writer.writeNext(header);
			writer.writeAll(IPPdelay);
			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
