package com.RINEX_parser.fileParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.SatelliteModel;

public class ObservationRNX {

	public static ArrayList<ObservationMsg> rinex_obsv_process(String path) {
		File file = new File(path);
		ArrayList<ObservationMsg> ObsvMsgs = new ArrayList<ObservationMsg>();
		try {
			Scanner input = new Scanner(file);
			input.useDelimiter("HEADER");

			Scanner header = new Scanner(input.next());

			double[] ECEF_XYZ = new double[3];
			ArrayList<String> obs_types = new ArrayList<String>();
			while (header.hasNextLine()) {
				// Remove leading and trailing whitespace, as split method adds them
				String line = header.nextLine().trim();
				if (line.contains("APPROX POSITION XYZ")) {
					ECEF_XYZ = Arrays.stream(line.split("\\s+")).limit(3).mapToDouble(x -> Double.parseDouble(x))
							.toArray();

				} else if (line.contains("SYS / # / OBS TYPES")) {
					obs_types.addAll(Arrays.asList(line.replaceAll("SYS / # / OBS TYPES", "").split("\\s+")));
				}
			}

			int GPSindex = obs_types.indexOf("G");
			int L1C_index = obs_types.subList(GPSindex + 2, GPSindex + Integer.parseInt(obs_types.get(GPSindex + 1)))
					.indexOf("C1C");

			String[] obsv_msgs = input.next().trim().split(">");
			for (String msg : obsv_msgs) {
				if (msg.isBlank()) {
					continue;
				}
				ArrayList<SatelliteModel> SV = new ArrayList<SatelliteModel>();
				ObservationMsg Msg = new ObservationMsg();
				msg = msg.trim();
				String[] msgTokens = msg.split("\\R+");

				Arrays.stream(msgTokens).skip(1).filter(x -> x.trim().charAt(0) == 'G').map(x -> x.trim().split("\\s+"))
						.forEach(x -> SV.add(new SatelliteModel(x[0], x[1 + L1C_index])));

				Msg.set_ECEF_XYZ(ECEF_XYZ);
				Msg.set_RxTime(msgTokens[0].trim().split("\\s+"));
				Msg.setObsvSat(SV);

				ObsvMsgs.add(Msg);

			}

		} catch (

		FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ObsvMsgs;
	}

}
