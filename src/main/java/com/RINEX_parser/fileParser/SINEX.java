package com.RINEX_parser.fileParser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class SINEX {

	public static double[] sinex_process(String path, String siteCode) {
		File file = new File(path);
		try {
			Scanner input = new Scanner(file);
			input.useDelimiter("\\+SOLUTION/ESTIMATE|\\-SOLUTION/ESTIMATE");
			input.next();
			Scanner est = new Scanner(input.next());
			est.useDelimiter("\n");

			String[] fields = est.next().split("\\s+");
			// INDEX _TYPE_ CODE PT SOLN _REF_EPOCH__ UNIT S ___ESTIMATED_VALUE___
			// __STD_DEV__
			HashMap<String, Integer> fields_index = new HashMap<String, Integer>();
			for (int i = 0; i < fields.length; i++) {
				fields_index.put(fields[i], i);
			}
			HashMap<String, Double> stationInfo = new HashMap<String, Double>();
			while (est.hasNext()) {
				String[] values = est.next().trim().split("\\s+");
				if (values[fields_index.get("CODE")].trim().equalsIgnoreCase(siteCode.trim())) {
					stationInfo.put(values[fields_index.get("_TYPE_")].trim(),
							Double.parseDouble(values[fields_index.get("___ESTIMATED_VALUE___")].trim()));
				}
			}
			double[] stationXYZ = new double[] { stationInfo.get("STAX"), stationInfo.get("STAY"),
					stationInfo.get("STAZ") };
			input.close();
			return stationXYZ;

		} catch (

		IOException e) {
			// TODO Auto-generated catch block
			System.out.println(e);
			e.printStackTrace();
		}
		return null;
	}
}
