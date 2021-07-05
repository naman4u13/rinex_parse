package com.RINEX_parser.fileParser.GoogleDecimeter;

import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.RINEX_parser.models.GoogleDecimeter.Derived;
import com.opencsv.CSVReader;

public class DerivedCSV {
	private static final long NumberMilliSecondsWeek = 604800000;
	private static final Map<String, String> codeMap = Map.of("GPS_L1", "G1C", "GPS_L5", "G5X", "GAL_E1", "E1C",
			"GAL_E5A", "E5X", "GLO_G1", "R1C", "BDS_B1I", "C2I", "QZS_J1", "J1C", "QZS_J5", "J5X");
	private static final Map<Integer, Integer> QZSSprn = Map.of(193, 1, 194, 2, 199, 3, 195, 4);

	public static HashMap<Double, HashMap<String, HashMap<Integer, Derived>>> processCSV(String path) throws Exception {

		HashMap<Double, HashMap<String, HashMap<Integer, Derived>>> derivedMap = new HashMap<Double, HashMap<String, HashMap<Integer, Derived>>>();

		try {
			CSVReader reader = new CSVReader(new FileReader(path));
			String[] line;
			String[] header = reader.readNext();
			// reads one line at a time
			while ((line = reader.readNext()) != null) {
				double millisSinceGpsEpoch = Double.parseDouble(line[2]);
				double tRX = ((millisSinceGpsEpoch % NumberMilliSecondsWeek) / 1000);
				double weekNo = Math.floor(millisSinceGpsEpoch / NumberMilliSecondsWeek);
				int svid = Integer.parseInt(line[4]);
				String signalType = line[5];
				String obsvCode = codeMap.get(signalType);
				if (obsvCode.charAt(0) == 'J') {
					svid = QZSSprn.get(svid);
				}
				Derived derivedData = new Derived(Arrays.copyOfRange(line, 6, line.length));

				derivedMap.computeIfAbsent(tRX, k -> new HashMap<String, HashMap<Integer, Derived>>())
						.computeIfAbsent(obsvCode, k -> new HashMap<Integer, Derived>()).put(svid, derivedData);

			}
			reader.close();

		} catch (Exception e) {
			// TODO: handle exception
			throw new Exception("Error occured during parsing of Derived CSV file \n" + e);

		}
		return derivedMap;

	}

}
