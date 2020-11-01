package com.RINEX_parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.RINEX_parser.ComputeUserPos.LeastSquare;
import com.RINEX_parser.fileParser.NavigationRNX;
import com.RINEX_parser.fileParser.ObservationRNX;
import com.RINEX_parser.helper.ComputeSatPos;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.NavigationMsg;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.SatelliteModel;
import com.RINEX_parser.models.TimeCorrection;
import com.RINEX_parser.utility.Closest;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.LatLonDiff;

public class MainApp {

	public static void main(String[] args) {

		double SpeedofLight = 299792458;

		String nav_path = "C:\\Users\\Naman\\Downloads\\BRDC00IGS_R_20201000000_01D_MN.rnx\\BRDC00IGS_R_20201000000_01D_MN.rnx";

		String obs_path = "C:\\Users\\Naman\\Downloads\\TWTF00TWN_R_20201000000_01D_30S_MO.rnx\\TWTF00TWN_R_20201000000_01D_30S_MO.rnx";

		Map<String, Object> NavMsgComp = NavigationRNX.rinex_nav_process(nav_path);
		File output = new File("C:\\Users\\Naman\\Desktop\\output_TWTF_BRDC.txt");
		PrintStream stream;
		try {
			stream = new PrintStream(output);
			System.setOut(stream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		@SuppressWarnings("unchecked")
		HashMap<Integer, ArrayList<NavigationMsg>> NavMsgs = (HashMap<Integer, ArrayList<NavigationMsg>>) NavMsgComp
				.get("NavMsgs");
		IonoCoeff ionoCoeff = (IonoCoeff) NavMsgComp.get("ionoCoeff");
		TimeCorrection timeCorr = (TimeCorrection) NavMsgComp.get("timeCorr");

		ArrayList<ObservationMsg> ObsvMsgs = ObservationRNX.rinex_obsv_process(obs_path);
		double minErr = Double.MAX_VALUE;
		double _minErr = Double.MAX_VALUE;
		double avgErr = 0;
		double _avgErr = 0;
		double minLLdiff = Double.MAX_VALUE;
		double avgLLdiff = 0;
		for (ObservationMsg obsvMsg : ObsvMsgs) {

			long tRX = obsvMsg.getTRX();
			double[] userECEF = obsvMsg.getECEF();

			int _order[] = obsvMsg.getObsvSat().stream().map(i -> NavMsgs.get(i.getSVID()))
					.map(i -> (ArrayList<Long>) i.stream().map(j -> j.getTOC()).collect(Collectors.toList()))
					.mapToInt(i -> Closest.findClosest(tRX, i)).toArray();
			// find out index of nav-msg inside the nav-msg list which is most suitable for
			// each obs-msg based on time
			int[] order = obsvMsg.getObsvSat().stream().map(i -> NavMsgs.get(i.getSVID()))
					.map(i -> (ArrayList<Long>) i.stream().map(j -> j.getTOC()).collect(Collectors.toList()))
					.mapToInt(i -> Closest.findClosest_BS(tRX, i)).toArray();
			// Create a mapping for each obs-msg, such that SVID ->
			// SV-position&clockOffset-data
			ArrayList<Satellite> SV = new ArrayList<Satellite>();
			ArrayList<Satellite> _SV = new ArrayList<Satellite>();
			System.out.print("\n");
			Arrays.stream(order).forEach(x -> System.out.print(x + " "));
			System.out.print("\n");
			Arrays.stream(_order).forEach(x -> System.out.print(x + " "));
			System.out.print("\n");

			for (int i = 0; i < order.length; i++) {

				SatelliteModel sat = obsvMsg.getObsvSat().get(i);
				int SVID = sat.getSVID();
				double tSV = tRX - (sat.getPseudorange() / SpeedofLight);
				double PseudoRange = obsvMsg.getObsvSat().get(i).getPseudorange();
				double[] ECEF_SatClkOff = ComputeSatPos.computeSatPos(NavMsgs.get(SVID).get(order[i]), tSV);
				SV.add(new Satellite(SVID, PseudoRange, Arrays.copyOfRange(ECEF_SatClkOff, 0, 3), ECEF_SatClkOff[3]));
				double[] _ECEF_SatClkOff = ComputeSatPos.computeSatPos(NavMsgs.get(SVID).get(_order[i]), tSV);
				_SV.add(new Satellite(SVID, PseudoRange, Arrays.copyOfRange(_ECEF_SatClkOff, 0, 3),
						_ECEF_SatClkOff[3]));

			}

			double[] computedECEF = LeastSquare.compute(SV);
			double[] _computedECEF = LeastSquare.compute(_SV);
			if (computedECEF.length == 3) {
				double Error = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> userECEF[x] - computedECEF[x])
						.map(x -> Math.pow(x, 2)).reduce(0, (a, b) -> a + b));
				double _Error = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> userECEF[x] - _computedECEF[x])
						.map(x -> Math.pow(x, 2)).reduce(0, (a, b) -> a + b));

				double[] computedLatLon = ECEFtoLatLon.ecef2lla(computedECEF);
				double[] userLatLon = ECEFtoLatLon.ecef2lla(userECEF);
				double diff = LatLonDiff.distance(computedLatLon, userLatLon);

				System.out.println(obsvMsg.getHour() + "  " + obsvMsg.getMinute() + "  " + obsvMsg.getSecond() + "  "
						+ Error + "  " + _Error + "   LL Diff - " + diff);
				minErr = Math.min(Error, minErr);
				_minErr = Math.min(_Error, _minErr);
				avgErr += Error;
				_avgErr += _Error;
				minLLdiff = Math.min(diff, minLLdiff);
				avgLLdiff += diff;

			}

		}
		int totalObsCount = ObsvMsgs.size();
		System.out.println(minErr + "  " + _minErr + "  " + minLLdiff);
		System.out.println(avgErr / totalObsCount + "  " + _avgErr / totalObsCount + "  " + avgLLdiff / totalObsCount);

	}

}
