package com.RINEX_parser.fileParser;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Scanner;
import java.util.stream.IntStream;

import com.RINEX_parser.helper.ComputeIPP;
import com.RINEX_parser.utility.LatLonUtil;
import com.RINEX_parser.utility.StringUtil;

public class IONEX {
	private double[][][] vtecMap;
	private double interval;
	private double Re;
	private double h;
	private double latDel;
	private double lonDel;
	private double exp;

	public IONEX(String path) throws Exception {
		process(path);
	}

	private void process(String path) throws Exception {

		try {

			File file = new File(path);
			Scanner input = new Scanner(file);
			input.useDelimiter("END OF HEADER");
			String[] header = input.next().split("\\n+");

			int n = 0;
			double[] latParam = null;
			double[] lonParam = null;

			for (String line : header) {
				String[] data = StringUtil.splitter(line, false, 60);

				if (data[1].equalsIgnoreCase("INTERVAL")) {
					interval = Double.parseDouble(data[0]);
					n = ((int) (86400 / interval)) + 1;
				} else if (data[1].equalsIgnoreCase("BASE RADIUS")) {
					Re = Double.parseDouble(data[0]) * 1000;

				} else if (data[1].equalsIgnoreCase("HGT1 / HGT2 / DHGT")) {

					String[] arr = StringUtil.splitter(line, false, 2, 6, 6, 6);
					double dhgt = Double.parseDouble(arr[3]);
					if (dhgt != 0) {
						throw new Exception("The GIM is 3D Map, Height is not constant");
					}
					h = Double.parseDouble(arr[1]) * 1000;

				} else if (data[1].equalsIgnoreCase("LAT1 / LAT2 / DLAT")) {
					String[] arr = StringUtil.splitter(line, false, 2, 6, 6, 6);
					latParam = IntStream.range(1, 4).mapToDouble(i -> Double.parseDouble(arr[i])).toArray();
				} else if (data[1].equalsIgnoreCase("LON1 / LON2 / DLON")) {
					String[] arr = StringUtil.splitter(line, false, 2, 6, 6, 6);
					lonParam = IntStream.range(1, 4).mapToDouble(i -> Double.parseDouble(arr[i])).toArray();
				} else if (data[1].equalsIgnoreCase("EXPONENT")) {
					exp = Double.parseDouble(data[0]);
					break;
				}
			}
			int latN = (int) Math.abs((latParam[1] - latParam[0]) / latParam[2]) + 1;
			int lonN = (int) Math.abs((lonParam[1] - lonParam[0]) / lonParam[2]) + 1;
			latDel = Math.abs(latParam[2]);
			lonDel = Math.abs(lonParam[2]);
			// Max record length is 80 and each TEC value takes 5 unit length
			int lineN = (5 * lonN) / 80 + 1;
			vtecMap = new double[n][latN + 2][];

			String[] lines = input.next().split("\\n+");

			int p = -1;
			int q = latN;
			for (int i = 1; i < lines.length; i++) {
				String[] data = StringUtil.splitter(lines[i], false, 60);
				if (data[1].equalsIgnoreCase("START OF TEC MAP")) {
					p++;
					q = latN;
					// intialize north pole and south pole - lat +/- 90 with zero value
					vtecMap[p][0] = new double[lonN];
					vtecMap[p][latN + 1] = new double[lonN];

				} else if (data[1].equalsIgnoreCase("LAT/LON1/LON2/DLON/H")) {
					String tecStr = "";
					for (int j = 1; j <= lineN; j++) {
						i++;
						tecStr += "\s" + lines[i];
					}
					double[] tec = null;

					tec = Arrays.stream(tecStr.trim().split("\\s+")).mapToDouble(j -> Double.parseDouble(j)).toArray();
					if (tec.length != lonN) {
						throw new Exception(
								"Error occured during parsing of IONEX file, Longitude params have not parsed properly \n");
					}
					vtecMap[p][q] = tec;
					q--;

				} else if (data[1].equalsIgnoreCase("END OF TEC MAP")) {
					if (p >= n - 1) {
						break;
					}
				}
			}

		} catch (Exception e) {
			throw new Exception("Error occured during parsing of IONEX file \n" + e);
		}

	}

	public double computeIonoCorr(double ElevAng_rad, double AzmAng_rad, double userLat_deg, double userLong_deg,
			double GPSTime, double freq, Calendar time) {

		double[] IPP = ComputeIPP.computeIPP(ElevAng_rad, AzmAng_rad, userLat_deg, userLong_deg, Re, h);

		if (IPP[0] > 90 || IPP[0] < -90 || IPP[1] > 180 || IPP[1] < -180) {
			System.err.println("IPP computation gone wrong !");
		}
		double vtec = interpolate(IPP, GPSTime);
		vtec = vtec * Math.pow(10, exp);

		// Obliquity Factor
		double Fpp = 1 / Math.sqrt(1 - Math.pow((Re * Math.cos(ElevAng_rad)) / (Re + h), 2));

		// Slant TEC
		double stec = Fpp * vtec;

		double ionoErr = (40.3 * (1E16) / Math.pow(freq, 2)) * stec;

		return ionoErr;

	}

	public double interpolate(double[] IPP, double GPSTime) {
		double t = GPSTime % 86400;
		double x = t / interval;

		int x1 = (int) Math.floor(x);
		int x2 = x1 + 1;
		double t1 = x1 * interval;
		double t2 = x2 * interval;

		double lat = IPP[0];
		double lon = IPP[1];

		double lon1 = LatLonUtil.lonAddDD(lon, ((t - t1) * 360 / 86400));
		double lon2 = LatLonUtil.lonAddDD(lon, ((t - t2) * 360 / 86400));

		double y = (lat / latDel) + (90 / latDel);
		int y1 = (int) Math.floor(y);
		int y2 = y1 + 1;

		double z1 = (lon1 / lonDel) + (180 / lonDel);
		int z11 = (int) Math.floor(z1);
		int z12 = z11 + 1;

		double z2 = (lon2 / lonDel) + (180 / lonDel);
		int z21 = (int) Math.floor(z2);
		int z22 = z21 + 1;

		double p1 = z1 - z11;
		double p2 = z2 - z21;
		double q = y - y1;

		double[][] E1 = vtecMap[x1];
		double[][] E2 = vtecMap[x2];

		double e1 = ((1 - p1) * (1 - q) * E1[y1][z11]) + (p1 * (1 - q) * E1[y1][z12]) + ((1 - p1) * q * E1[y2][z11])
				+ (p1 * q * E1[y2][z12]);

		double e2 = ((1 - p2) * (1 - q) * E2[y1][z21]) + (p2 * (1 - q) * E2[y1][z22]) + ((1 - p2) * q * E2[y2][z21])
				+ (p2 * q * E2[y2][z22]);

		double e = (((t2 - t) / interval) * e1) + (((t - t1) / interval) * e2);
		return e;
	}

	public void printMap() {
		for (int i = 0; i < vtecMap.length; i++) {
			System.out.println((i + 1) + " START OF TEC MAP");
			for (double lat = -90; lat <= 90; lat += 2.5) {
				int j = (int) ((lat / latDel) + (90 / latDel));
				System.out.println("LAT - " + lat);
				int l = 0;
				for (double lon = -180; lon <= 180; lon += 5) {
					int k = (int) ((lon / lonDel) + (180 / lonDel));
					double val = vtecMap[i][j][k];
					System.out.print(val + " ");
					l++;
					if (l == 16) {
						l = 0;
						System.out.println();
					}

				}
				System.out.println();
			}
			System.out.println("END OF TEC MAP");
			System.out.println();
		}
	}

}
