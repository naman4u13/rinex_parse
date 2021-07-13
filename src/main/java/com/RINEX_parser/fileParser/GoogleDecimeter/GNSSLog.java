package com.RINEX_parser.fileParser.GoogleDecimeter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.RINEX_parser.models.GoogleDecimeter.AndroidObsv;

public class GNSSLog {

	private static Map<Integer, String> freqMap = Map.of(1176450050, "5", 1575420030, "1", 1602000000, "1", 1561097980,
			"2");
	private static Map<Integer, String> SSIMap = Map.of(1, "G", 2, "S", 3, "R", 4, "J", 5, "C", 6, "E", 7, "I");

	public static void process(String path) throws Exception {
		ArrayList<String[]> raw = new ArrayList<String[]>();
		HashMap<Long, HashMap<String, AndroidObsv>> map = new HashMap<Long, HashMap<String, AndroidObsv>>();
		String[] rawFields = null;
		try {
			Path fileName = Path.of(path);
			String[] input = Files.readString(fileName).split("#");

			for (int i = 0; i < input.length - 1; i++) {
				if (input[i].trim().isBlank()) {
					continue;
				}
				String[] fields = input[i].trim().split(",");
				if (fields[0].equals("Raw")) {
					rawFields = fields;
					break;
				}
			}
			String[] lines = input[input.length - 1].trim().split("\r\n|\r|\n");
			HashSet<Double> freqSet = new HashSet<Double>();
			HashSet<String> codeSet = new HashSet<String>();
			for (String line : lines) {
				String[] data = line.trim().split(",");
				if (data[0].equals("Raw")) {
					raw.add(data);
					long timeNanos = Long.parseLong(data[2]);
					long fullBiasNanos = Long.parseLong(data[5]);
					double biasNanos = Double.parseDouble(data[6]);
					double timeOffNanos = Double.parseDouble(data[12]);
					int constellationType = Integer.parseInt(data[28]);

					int freq = (int) (Double.parseDouble(data[22]));
					if (freq >= 1598062500 && freq <= 1608750000) {
						freq = (int) 1602e6;
					}
					freqSet.add(Double.parseDouble(data[22]));
					String freqID = freqMap.get(freq);
					String channel = freqID == "1" ? "C" : freqID == "2" ? "I" : "X";

					String obsvCode = SSIMap.get(constellationType) + freqID + channel;
					long tRX = Math.round((timeNanos + timeOffNanos - (fullBiasNanos + biasNanos)) / 1e3);
					map.computeIfAbsent(tRX, k -> new HashMap<String, AndroidObsv>()).put(obsvCode,
							new AndroidObsv(data[7], data[19], data[20], data[21], data[26], data[36]));
					codeSet.add(obsvCode);
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			throw new Exception(e);
		}

	}

}
