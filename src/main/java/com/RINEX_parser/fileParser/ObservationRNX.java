package com.RINEX_parser.fileParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.RINEX_parser.constants.Constellation;
import com.RINEX_parser.models.Observable;
import com.RINEX_parser.models.ObservationMsg;

public class ObservationRNX {

	public static HashMap<String, Object> rinex_obsv_process(String path, boolean useSNX, String sinex_path,
			String[] obsvCode, boolean phaseReq, boolean dopplerReq) throws Exception {
		File file = new File(path);
		ArrayList<ObservationMsg> ObsvMsgs = new ArrayList<ObservationMsg>();
		Map<String, Object> ARP_PCO = null;
		int interval = 0;
		try {
			Scanner input = new Scanner(file);

			input.useDelimiter("HEADER");

			Scanner header = new Scanner(input.next());
			int fN = obsvCode.length;

			String siteCode = null;
			HashMap<Character, HashSet<String>> availObs = new HashMap<Character, HashSet<String>>();
			HashMap<Character, HashMap<String, Integer>> type_index_map = new HashMap<Character, HashMap<String, Integer>>();
			ArrayList<String> typeList = new ArrayList<String>();
			while (header.hasNextLine()) {

				// Remove leading and trailing whitespace, as split method adds them
				String line = header.nextLine().trim();
				if (line.contains("MARKER NAME")) {
					siteCode = line.split("\\s+")[0].trim().substring(0, 4);

				} else if (line.contains("APPROX POSITION XYZ")) {
					double[] ECEF_XYZ = Arrays.stream(line.split("\\s+")).limit(3)
							.mapToDouble(x -> Double.parseDouble(x)).toArray();
					// Note ECEF_XYZ is MM not ARP, PCO is not zero
					ARP_PCO = Map.of("ARP", ECEF_XYZ, "PCO", new double[obsvCode.length][3]);

				} else if (line.contains("SYS / # / OBS TYPES")) {
					typeList.addAll(Arrays.stream(line.replaceAll("SYS / # / OBS TYPES", "").split("\\s+"))
							.map(x -> x.trim()).collect(Collectors.toList()));

				} else if ((line.contains("INTERVAL"))) {
					interval = (int) Double.parseDouble(line.split("\\s+")[0]);

				}
			}

			for (int i = 0; i < typeList.size();) {
				char SSI = typeList.get(i).charAt(0);
				int count = Integer.parseInt(typeList.get(i + 1));
				String[] type_arr = typeList.subList(i + 2, i + 2 + count).toArray(String[]::new);
				HashMap<String, Integer> type_index = new HashMap<String, Integer>();
				HashSet<String> avail = new HashSet<String>();
				for (int j = 0; j < count; j++) {
					String code = type_arr[j].trim();
					type_index.put(code, j);
					if (code.charAt(0) == 'C') {
						avail.add(code.substring(1));
					}

				}

				type_index_map.put(SSI, type_index);
				availObs.put(SSI, avail);
				i += 2 + count;
			}

			if (useSNX) {
				ARP_PCO = SINEX.sinex_process(sinex_path, siteCode, obsvCode);

			}

			String[] obsv_msgs = input.next().trim().split(">");
			for (String msg : obsv_msgs) {
				if (msg.isBlank()) {
					continue;
				}

				HashMap<Character, HashMap<Integer, HashMap<Character, ArrayList<Observable>>>> SV = new HashMap<Character, HashMap<Integer, HashMap<Character, ArrayList<Observable>>>>();
				ObservationMsg Msg = new ObservationMsg();
				msg = msg.trim();

				String[] msgLines = msg.split("\\R+");
				for (int i = 1; i < msgLines.length; i++) {
					String msgLine = msgLines[i];
					String SVID = msgLine.substring(0, 3);
					char SSI = SVID.charAt(0);// Satellite System Indentifier
					int obsSize = type_index_map.get(SSI).size();
					String[] obsvs = new String[obsSize];
					msgLine = msgLine.substring(3);
					String[] tokens = msgLine.split("(?<=\\G.{16})");

					for (int j = 0; j < tokens.length; j++) {
						String token = tokens[j];
						token = token.trim();
						if (token.isBlank()) {
							obsvs[j] = null;
						} else {
							obsvs[j] = token.split("\\s+")[0].trim();

						}

					}

					HashMap<String, Integer> type_index = type_index_map.get(SSI);
					for (String str : availObs.get(SSI)) {
						int freqID = Integer.parseInt(str.charAt(0) + "");
						char codeID = str.charAt(1);
						String frequency = Constellation.frequency.get(SSI).get(freqID) + "";

						String pseudorange = type_index.containsKey('C' + str) ? obsvs[type_index.get('C' + str)]
								: null;
						String CNo = type_index.containsKey('S' + str) ? obsvs[type_index.get('S' + str)] : null;
						String phase = type_index.containsKey('L' + str) ? obsvs[type_index.get('L' + str)] : null;
						String doppler = type_index.containsKey('D' + str) ? obsvs[type_index.get('D' + str)] : null;
						if ((pseudorange == null || CNo == null) || (phase == null && phaseReq)
								|| (doppler == null && dopplerReq)) {
							SV.computeIfAbsent(SSI,
									k -> new HashMap<Integer, HashMap<Character, ArrayList<Observable>>>())
									.computeIfAbsent(freqID, k -> new HashMap<Character, ArrayList<Observable>>())
									.computeIfAbsent(codeID, k -> new ArrayList<Observable>()).add(null);
							continue;

						}

						SV.computeIfAbsent(SSI, k -> new HashMap<Integer, HashMap<Character, ArrayList<Observable>>>())
								.computeIfAbsent(freqID, k -> new HashMap<Character, ArrayList<Observable>>())
								.computeIfAbsent(codeID, k -> new ArrayList<Observable>())
								.add(new Observable(SSI, SVID, pseudorange, CNo, doppler, phase, frequency));

					}

				}

				Msg.set_RxTime(msgLines[0].trim().split("\\s+"));

				Msg.setObsvSat(SV);
				ObsvMsgs.add(Msg);

			}
			input.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Error occured during parsing of Observation RINEX(.rnx) file \n" + e);

		}
		HashMap<String, Object> result = new HashMap<String, Object>();
		result.putAll(ARP_PCO);
		result.put("ObsvMsgs", ObsvMsgs);
		result.put("interval", interval);
		return result;
	}

}
