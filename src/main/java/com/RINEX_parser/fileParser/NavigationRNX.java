package com.RINEX_parser.fileParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.NavigationMsg;
import com.RINEX_parser.models.TimeCorrection;
import com.RINEX_parser.utility.SymbolToken;

public class NavigationRNX {

	public static Map<String, Object> rinex_nav_process(String path, boolean getIonoOnly) throws Exception {

		File file = new File(path);

		HashMap<Integer, ArrayList<NavigationMsg>> SV = new HashMap<Integer, ArrayList<NavigationMsg>>();
		IonoCoeff ionoCoeff = new IonoCoeff();
		TimeCorrection timeCorr = new TimeCorrection();

		try {
			Scanner input = new Scanner(file);
			input.useDelimiter("HEADER");
			Scanner header = new Scanner(input.next());

			while (header.hasNextLine()) {
				// Remove leading and trailing whitespace, as split method adds them
				String line = header.nextLine().trim();
				if (line.contains("IONOSPHERIC CORR")) {
					if (line.contains("GPSA")) {
						// Split the line using whitespace delimiter and take out a subarray containing
						// the concerned 4 Iono params
						double[] GPSA = Arrays.stream(line.split("\\s+")).skip(1).limit(4)
								.mapToDouble(x -> Double.parseDouble(x)).toArray();
						ionoCoeff.setGPSA(GPSA);
					} else if (line.contains("GPSB")) {
						double[] GPSB = Arrays.stream(line.split("\\s+")).skip(1).limit(4)
								.mapToDouble(x -> Double.parseDouble(x)).toArray();
						ionoCoeff.setGPSB(GPSB);
						if (getIonoOnly) {

							return Map.of("ionoCoeff", ionoCoeff);
						}
					}
				} else if (line.contains("TIME SYSTEM CORR") && line.contains("GPUT")) {

					String[] GPUT = Arrays.stream(line.split("\\s+")).flatMap(x -> SymbolToken.split(x).stream())
							.skip(1).limit(4).toArray(String[]::new);
					timeCorr.setGPUT(GPUT);

				} else if (line.contains("LEAP SECONDS")) {

					timeCorr.setLeapSeconds(Long.parseLong(line.split("\\s+")[0]));
					// All the necessary has been acquired therefore we can terminate the loop and
					// stop reading the header
					break;
				}
			}

			String[] nav_msgs = input.next().split("\r\n|\r|\n");
			int lineNum = 0;
			while (lineNum < nav_msgs.length) {
				if (nav_msgs[lineNum].isBlank()) {
					lineNum++;
					continue;
				}
				String SVID = nav_msgs[lineNum].trim().split("\\s+")[0];

				if (SVID.charAt(0) == 'G') {
					String[] nav_data = Arrays.stream(Arrays.copyOfRange(nav_msgs, lineNum, lineNum + 8))
							.flatMap(x -> Arrays.stream(x.trim().split("\\s+")))
							.flatMap(x -> SymbolToken.split(x).stream()).toArray(String[]::new);
					int _SVID = Integer.parseInt(SVID.replaceAll("[a-zA-Z]", ""));

					SV.computeIfAbsent(_SVID, k -> new ArrayList<NavigationMsg>()).add(new NavigationMsg(nav_data));

					lineNum = lineNum + 8;
				} else {
					break;
				}
			}
			SV.forEach((k, v) -> v.sort((x, y) -> (int) ((x.getTOC() - y.getTOC()))));
			input.close();
			// Create a Object map to return multiple values each of different type
			Map<String, Object> resMap = Map.of("NavMsgs", SV, "ionoCoeff", ionoCoeff, "timeCorr", timeCorr);
			return resMap;

		} catch (

		Exception e) {
			e.printStackTrace();
			throw new Exception("Error occured during parsing of Navigation RINEX(.rnx) file \n" + e);
		}

	}
}
