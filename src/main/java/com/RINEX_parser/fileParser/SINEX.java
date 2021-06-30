package com.RINEX_parser.fileParser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.RINEX_parser.utility.LatLonUtil;
import com.RINEX_parser.utility.StringUtil;

public class SINEX {

	public static Map<String, Object> sinex_process(String path, String siteCode, String[] obsvCode) throws Exception {
		File file = new File(path);
		try {
			siteCode = siteCode.trim();

			String[] antennaType = new String[2];
			// Phase Center Offset
			HashMap<Character, HashMap<Integer, double[]>> PCO = new HashMap<Character, HashMap<Integer, double[]>>();
			// Vector distance from Monument Marker to ARP(Antenna Reference Point)
			double[] deltaARP = new double[3];
			// Eccentricity Reference System
			String refSys;
			// Monument Marker
			double[] MM = new double[3];

			Scanner input = new Scanner(file);
			input.useDelimiter("\\*-------------------------------------------------------------------------------");
			input.next();// Skip first line
			while (input.hasNext()) {
				String block = input.next();
				String[] lines = block.split("\\n+");
				String blockTitle = lines[1].trim();
				if (blockTitle.equals("+SITE/ANTENNA")) {
					// *CODE PT SOLN T _DATA START_ __DATA_END__ ____ANTENNA_TYPE____ _S/N_
					for (int i = 3; i < lines.length - 1; i++) {

						// last column S/N have variable length, therefore use mod splitter
						String[] values = StringUtil.splitter(lines[i], false, 5, 3, 5, 2, 13, 13, 21);
						if (values[0].equalsIgnoreCase(siteCode)) {
							antennaType = values[6].split("\\s+");

							break;

						}

					}

				} else if (blockTitle.equals("+SITE/GPS_PHASE_CENTER")) {
					// *________TYPE________ _S/N_ _L1_U_ _L1_N_ _L1_E_ _L2_U_ _L2_N_ _L2_E_
					// __MODEL___
					for (int i = 3; i < lines.length - 1; i++) {

						// last column can have variable length, therefore use mod splitter
						String[] values = StringUtil.splitter(lines[i], false, 21, 6, 7, 7, 7, 7, 7, 7);
						String[] _antennaType = values[0].split("\\s+");
						if (_antennaType[0].equals(antennaType[0]) && _antennaType[1].equals(antennaType[1])) {
							PCO.put('G', new HashMap<Integer, double[]>());
							double u1 = Double.parseDouble(values[2]);
							double n1 = Double.parseDouble(values[3]);
							double e1 = Double.parseDouble(values[4]);
							double u2 = Double.parseDouble(values[5]);
							double n2 = Double.parseDouble(values[6]);
							double e2 = Double.parseDouble(values[7]);
							PCO.get('G').put(1, new double[] { e1, n1, u1 });
							PCO.get('G').put(2, new double[] { e2, n2, u2 });

							break;
						}

					}
				} else if (blockTitle.equals("+SITE/ECCENTRICITY")) {
					// *CODE PT SOLN T _DATA START_ __DATA_END__ REF __DX_U__ __DX_N__ __DX_E__
					for (int i = 3; i < lines.length - 1; i++) {

						String[] values = StringUtil.splitter(lines[i], 5, 3, 5, 2, 13, 13, 4, 9, 9, 9);
						if (values[0].equalsIgnoreCase(siteCode)) {
							refSys = values[6];
							double u = Double.parseDouble(values[7]);
							double n = Double.parseDouble(values[8]);
							double e = Double.parseDouble(values[9]);
							deltaARP = new double[] { e, n, u };

							break;

						}

					}

				} else if (blockTitle.equals("+SOLUTION/ESTIMATE")) {
					String[] fields = lines[2].split("\\s+");
					// INDEX _TYPE_ CODE PT SOLN _REF_EPOCH__ UNIT S ___ESTIMATED_VALUE___
					// __STD_DEV__
					HashMap<String, Integer> fields_index = new HashMap<String, Integer>();
					for (int i = 0; i < fields.length; i++) {
						fields_index.put(fields[i], i);
					}
					HashMap<String, Double> stationInfo = new HashMap<String, Double>();
					for (int i = 3; i < lines.length - 1; i++) {

						String[] values = StringUtil.splitter(lines[i], 6, 7, 5, 3, 5, 13, 5, 2, 22, 12);
						if (values[fields_index.get("CODE")].equalsIgnoreCase(siteCode)) {
							stationInfo.put(values[fields_index.get("_TYPE_")].trim(),
									Double.parseDouble(values[fields_index.get("___ESTIMATED_VALUE___")].trim()));
						}
					}
					MM = new double[] { stationInfo.get("STAX"), stationInfo.get("STAY"), stationInfo.get("STAZ") };

				}

			}
			input.close();
			return compute_ARP_PCO(MM, deltaARP, PCO, obsvCode);

		} catch (

		IOException e) {
			throw new Exception("Error occured during parsing of Sinex(.snx) file \n" + e);
		}

	}

	private static Map<String, Object> compute_ARP_PCO(double[] MM, double[] deltaARP,
			HashMap<Character, HashMap<Integer, double[]>> PCO, String[] obsvCode) {
		int fN = obsvCode.length;
		double[][] PCO_ECEF = new double[fN][3];

		double[] ARP = LatLonUtil.ENUtoECEF(deltaARP, MM);
		for (int i = 0; i < fN; i++) {
			char SSI = obsvCode[i].charAt(0);
			int freq = Integer.parseInt(obsvCode[i].charAt(1) + "");
			double[] pco = PCO.get(SSI).get(freq);

			if (pco == null) {
				System.err.println("Rx PCO info unavailable for frequency - " + freq + " !");

				PCO_ECEF[i] = new double[] { 0, 0, 0 };
				continue;
			}

			double[] APC = LatLonUtil.ENUtoECEF(pco, ARP);
			for (int j = 0; j < 3; j++) {
				PCO_ECEF[i][j] = APC[j] - ARP[j];
			}

		}
		return Map.of("ARP", ARP, "PCO", PCO_ECEF);
	}
}
