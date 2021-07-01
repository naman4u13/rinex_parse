package com.RINEX_parser.fileParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

import com.RINEX_parser.models.IGS.IGSOrbit;
import com.RINEX_parser.utility.Interpolator;
import com.RINEX_parser.utility.StringUtil;
import com.RINEX_parser.utility.Time;

public class Orbit {

	private ArrayList<IGSOrbit> IGSOrbitList;
	private int[] pts;

	public Orbit(String path, char SSI) throws Exception {
		HashMap<Character, ArrayList<IGSOrbit>> IGSOrbitMap = orbit_process(path);

		IGSOrbitList = IGSOrbitMap.get(SSI);
	}

	private HashMap<Character, ArrayList<IGSOrbit>> orbit_process(String path) throws Exception {
		HashMap<Character, ArrayList<IGSOrbit>> IGSOrbitMap = new HashMap<Character, ArrayList<IGSOrbit>>();
		try {
			Path fileName = Path.of(path);
			String input = Files.readString(fileName);
			String[] lines = input.split("\r\n|\r|\n");
			int satCount = Integer.parseInt(StringUtil.splitter(lines[2], false, 4, 2)[1]);

			// "EOF" is last line of Orbit file, which will be excluded
			lines = Arrays.copyOfRange(lines, 22, lines.length - 1);
			for (int i = 0; i < lines.length; i++) {
				HashMap<Character, HashMap<Integer, double[]>> satECEF = new HashMap<Character, HashMap<Integer, double[]>>();
				String[] strTime = StringUtil.splitter(lines[i], false, 3);
				strTime = strTime[1].split("\\s+");
				double[] time = Time.getGPSTime(strTime);
				double GPSTime = time[0];
				long weekNo = (long) time[1];
				for (int j = 0; j < satCount; j++) {
					i += 1;
					String[] line = StringUtil.splitter(lines[i], false, 1, 3, 14, 14, 14, 14);
					char symbol = line[0].charAt(0);
					if (symbol == 'P') {
						char constellation = line[1].charAt(0);
						int PRN = Integer.parseInt(line[1].substring(1));
						double x = Double.parseDouble(line[2]) * 1000;
						double y = Double.parseDouble(line[3]) * 1000;
						double z = Double.parseDouble(line[4]) * 1000;
						if (x == 0.0 || y == 0.0 || z == 0.0) {
							continue;
						}
						double satClkOff = Double.parseDouble(line[5]) * 1e-6;
						satECEF.computeIfAbsent(constellation, k -> new HashMap<Integer, double[]>()).put(PRN,
								new double[] { x, y, z, satClkOff });

					}

				}

				satECEF.forEach((key, value) -> IGSOrbitMap.computeIfAbsent(key, k -> new ArrayList<IGSOrbit>())
						.add(new IGSOrbit(GPSTime, value)));
			}
			return IGSOrbitMap;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Error occured during parsing of IGS Orbit(.sp3) file \n" + e);
			// TODO: handle exception
		}

	}

	public void findPts(double x, int n) {

		// Boundary Index
		pts = new int[2];
		int len = IGSOrbitList.size();
		int m = n / 2;
		for (int i = 0; i < len - 1; i++) {
			double x1 = IGSOrbitList.get(i).getTime();
			double x2 = IGSOrbitList.get(i + 1).getTime();
			if (x >= x1 && x < x2) {

				if (i + 1 >= m) {
					if (len - (i + 1) >= m) {
						pts[0] = i - (m - 1);
						pts[1] = i + m;
					} else {
						pts[0] = len - n;
						pts[1] = len - 1;
					}
				} else {
					pts[0] = 0;
					pts[1] = n - 1;
				}
				return;
			}
		}
		// Incase when time 'x' has exceeded the last timestamp in sp3 file
		pts[0] = len - n;
		pts[1] = len - 1;

	}

	public double[][] getPV(double x, int SVID, int n) {
		double[] X = new double[n];
		double[][] Y = new double[3][n];

		for (int i = pts[0]; i <= pts[1]; i++) {
			int index = i - pts[0];
			IGSOrbit orbit = IGSOrbitList.get(i);
			X[index] = orbit.getTime();
			double[] satECEF = orbit.getSatECEF().get(SVID);
			IntStream.range(0, 3).forEach(j -> Y[j][index] = satECEF[j]);

		}

		double[][] y = new double[2][3];
		for (int i = 0; i < 3; i++) {
			double[] temp = Interpolator.lagrange(X, Y[i], x, true);
			y[0][i] = temp[0];
			y[1][i] = temp[1];
		}
		return y;
	}

}
