package com.RINEX_parser.fileParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.SatelliteModel;

public class ObservationRNX {

	public static ArrayList<ObservationMsg> rinex_obsv_process(String path, boolean useSNX) {
		File file = new File(path);
		ArrayList<ObservationMsg> ObsvMsgs = new ArrayList<ObservationMsg>();
		try {
			Scanner input = new Scanner(file);

			input.useDelimiter("HEADER");

			Scanner header = new Scanner(input.next());

			double[] ECEF_XYZ = new double[3];
			String siteCode = null;
			ArrayList<String> obs_types = new ArrayList<String>();
			while (header.hasNextLine()) {

				// Remove leading and trailing whitespace, as split method adds them
				String line = header.nextLine().trim();
				if (line.contains("MARKER NAME")) {
					siteCode = line.split("\\s+")[0].trim().substring(0, 4);

				} else if (line.contains("APPROX POSITION XYZ")) {
					ECEF_XYZ = Arrays.stream(line.split("\\s+")).limit(3).mapToDouble(x -> Double.parseDouble(x))
							.toArray();

				} else if (line.contains("SYS / # / OBS TYPES")) {
					obs_types.addAll(Arrays.asList(line.replaceAll("SYS / # / OBS TYPES", "").split("\\s+")));
				}
			}
			if (useSNX) {
				String snxPath = "C:\\Users\\Naman\\Downloads\\igs20P21004.ssc\\igs20P21004.ssc";
				ECEF_XYZ = SINEX.sinex_process(snxPath, siteCode);
			}
			HashMap<String, Integer> type_index = new HashMap<String, Integer>();
			int GPSindex = obs_types.indexOf("G");

			for (int i = 0; i < Integer.parseInt(obs_types.get(GPSindex + 1)); i++) {
				String type = obs_types.get(GPSindex + 2 + i).trim();

				type_index.put(type, i);
			}

			String[] obsv_msgs = input.next().trim().split(">");
			for (String msg : obsv_msgs) {
				if (msg.isBlank()) {
					continue;
				}

				ArrayList<SatelliteModel> SV = new ArrayList<SatelliteModel>();
				ObservationMsg Msg = new ObservationMsg();
				msg = msg.trim();

				String[] msgLines = msg.split("\\R+");
				for (int i = 1; i < msgLines.length; i++) {
					String msgLine = msgLines[i];
					String SVID = msgLine.substring(0, 3);
					if (msgLine.trim().charAt(0) == 'G') {
						msgLine = msgLine.substring(3);
						String[] tokens = msgLine.split("(?<=\\G.{16})");
						ArrayList<String> satInfo = new ArrayList<String>();
						for (String token : tokens) {
							token = token.trim();
							if (token.isBlank()) {
								satInfo.add("");
							} else {
								satInfo.add(token.split("\\s+")[0].trim());
							}

						}
						try {
							String doppler = type_index.containsKey("D1C") ? satInfo.get(type_index.get("D1C")) : "0";

							SV.add(new SatelliteModel(SVID, satInfo.get(type_index.get("C1C")),
									satInfo.get(type_index.get("S1C")), doppler));
						} catch (Exception e) {
							// TODO: handle exception
							System.out.println("MAIN ERROR - " + e + "\n" + msgLine);

						}

					}
				}

				Msg.set_ECEF_XYZ(ECEF_XYZ);

				Msg.set_RxTime(msgLines[0].trim().split("\\s+"));
				Msg.setObsvSat(SV);
				ObsvMsgs.add(Msg);

			}
			input.close();

		} catch (

		FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ObsvMsgs;
	}

}
