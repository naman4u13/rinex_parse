package com.RINEX_parser.utility;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.IntStream;

import com.opencsv.CSVReader;

public class GoogleData {

	public static ArrayList<String[]> predict(ArrayList<String[]> data) {

		ArrayList<String[]> predictedData = new ArrayList<String[]>();
		int n = data.size();
		String phone = data.get(0)[0];
		long t0 = Long.parseLong(data.get(0)[1]);
		predictedData.add(data.get(0));

		for (int i = 1; i < n; i++) {
			long t = Long.parseLong(data.get(i)[1]);
			long diff = (t - t0) / 1000;
			final int _i = i;
			for (int j = 1; j <= (diff - 1); j++) {
				double[] ll1 = IntStream.range(2, 4).mapToDouble(k -> Double.parseDouble(data.get(_i - 1)[k]))
						.toArray();
				double[] ll2 = IntStream.range(2, 4).mapToDouble(k -> Double.parseDouble(data.get(_i)[k])).toArray();
				String lat = String.valueOf(((ll1[0] * (diff - j)) + (ll2[0] * j)) / diff);
				String lon = String.valueOf(((ll1[1] * (diff - j)) + (ll2[1] * j)) / diff);
				String time = String.valueOf(t0 + (j * 1000));
				predictedData.add(new String[] { phone, time, lat, lon });
			}
			predictedData.add(data.get(i));
			t0 = t;
		}

		return predictedData;

	}

	public static void filter(ArrayList<String[]> data) {

		try {
			String path = "E:\\Study\\Google Decimeter Challenge\\decimeter\\baseline_locations_test.csv";
			CSVReader reader = new CSVReader(new FileReader(path));
			String[] line;
			String[] header = reader.readNext();
			HashMap<String, ArrayList<Long>> map = new HashMap<String, ArrayList<Long>>();
			while ((line = reader.readNext()) != null) {
				String phone = line[0] + "_" + line[1];
				String time = line[2];

				map.computeIfAbsent(phone, k -> new ArrayList<Long>()).add(Long.parseLong(time));
			}
			data.removeIf(i -> !map.get(i[0]).contains(Long.parseLong(i[1])));
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
