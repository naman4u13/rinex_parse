package com.RINEX_parser.fileParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

import com.RINEX_parser.models.IGS.IGSOrbit;
import com.RINEX_parser.utility.StringUtil;
import com.RINEX_parser.utility.Time;

public class Orbit {

	public static HashMap<Character, ArrayList<IGSOrbit>> orbit_process(String path) throws Exception {
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
				String[] strTime = lines[i].split("\\s+");
				strTime[6] = strTime[6].split("\\.")[0];
				int[] tArr = IntStream.range(1, strTime.length).map(x -> Integer.parseInt(strTime[x])).toArray();

				long[] time = Time.getGPSTime(tArr[0] + 2000, tArr[1] - 1, tArr[2], tArr[3], tArr[4], tArr[5]);
				long GPSTime = time[0];
				long weekNo = time[1];
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
}
