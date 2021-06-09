package com.RINEX_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.RINEX_parser.fileParser.Antenna;
import com.RINEX_parser.fileParser.Bias;
import com.RINEX_parser.fileParser.Clock;
import com.RINEX_parser.fileParser.Orbit;
import com.RINEX_parser.helper.ComputeSatPos;
import com.RINEX_parser.models.NavigationMsg;
import com.RINEX_parser.models.Observable;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.Closest;

public class DualFreq {

	final static double SpeedofLight = 299792458;

	public static ArrayList<Satellite>[] process(ObservationMsg obsvMsg,
			HashMap<Integer, ArrayList<NavigationMsg>> NavMsgs, String[] obsvCode, boolean useIGS, boolean useBias,
			Bias bias, Orbit orbit, Clock clock, Antenna antenna, long tRX, long weekNo, Calendar time) {

		ArrayList<Observable> observables1 = obsvMsg.getObsvSat(obsvCode[0]);
		ArrayList<Observable> observables2 = obsvMsg.getObsvSat(obsvCode[1]);
		ArrayList<Satellite>[] SV = new ArrayList[2];
		SV[0] = new ArrayList<Satellite>();
		SV[1] = new ArrayList<Satellite>();
		int satCount = observables1.size();

		if (useIGS) {
			int polyOrder = 10;
			orbit.findPts(tRX, polyOrder);
			clock.findPts(tRX);
			for (int i = 0; i < satCount; i++) {
				Observable sat1 = observables1.get(i);
				Observable sat2 = observables2.get(i);
				if (sat1 == null || sat2 == null) {
					continue;
				}
				// PRN
				int SVID = sat1.getSVID();
				double tSV1 = tRX - (sat1.getPseudorange() / SpeedofLight);
				double tSV2 = tRX - (sat2.getPseudorange() / SpeedofLight);

				double[] satClkOff = clock.getBias(tSV1, SVID, obsvCode);

				// GPS System transmission time
				double t = tSV1 - satClkOff[0];

				double[][] satPV = orbit.getPV(t, SVID, polyOrder);
				double[] satECEF_MC = satPV[0];
				double[] satVel = satPV[1];

				double dotProd = 0;
				for (int j = 0; j < 3; j++) {
					dotProd += satECEF_MC[j] * satVel[j];
				}
				double relativistic_error = -2 * (dotProd) / Math.pow(SpeedofLight, 2);
				// Correct sat clock offset for relativistic error and recompute the Sat coords
				satClkOff[0] += relativistic_error;
				satClkOff[1] += relativistic_error;
				t = tSV1 - satClkOff[0];

				double[] satECEF_PC1 = antenna.getSatPC(SVID, obsvCode[0], tRX, weekNo, satECEF_MC);
				double[] satECEF_PC2 = antenna.getSatPC(SVID, obsvCode[1], tRX, weekNo, satECEF_MC);
				Satellite _sat1 = new Satellite(sat1, satECEF_PC1, satClkOff[0], t, tRX, satVel, 0.0, null, time);
				_sat1.compECI();
				Satellite _sat2 = new Satellite(sat2, satECEF_PC2, satClkOff[1], t, tRX, satVel, 0.0, null, time);
				_sat2.compECI();

				SV[0].add(_sat1);
				SV[1].add(_sat2);

			}

		} else {

			// find out index of nav-msg inside the nav-msg list which is most suitable for
			// each obs-msg based on time
			int[] order = new int[satCount];
			for (int i = 0; i < satCount; i++) {
				Observable obs = observables1.get(i);
				if (obs == null) {
					continue;
				}
				ArrayList<NavigationMsg> navMsg = NavMsgs.get(obs.getSVID());
				ArrayList<Long> TOCs = (ArrayList<Long>) navMsg.stream().map(j -> j.getTOC())
						.collect(Collectors.toList());
				order[i] = Closest.findClosest(tRX, TOCs);
			}

			for (int i = 0; i < order.length; i++) {

				Observable sat1 = observables1.get(i);
				Observable sat2 = observables2.get(i);
				// PRN
				if (sat1 == null || sat2 == null) {
					continue;
				}
				int SVID = sat1.getSVID();

				// IGS .BSX file DCB
				double ISC1 = 0;
				double ISC2 = 0;

				NavigationMsg NavMsg = NavMsgs.get(SVID).get(order[i]);

				if (useBias) {
					ISC1 = bias.getISC(obsvCode[0], SVID);
					ISC2 = bias.getISC(obsvCode[1], SVID);
				}

				double tSV1 = tRX - (sat1.getPseudorange() / SpeedofLight);

				Object[] SatParams1 = ComputeSatPos.computeSatPos(NavMsg, tSV1, tRX, null, ISC1);
				double[] ECEF_SatClkOff1 = (double[]) SatParams1[0];
				double[] SatVel1 = (double[]) SatParams1[1];
				// Note this Clock Drift is derived, it not what we get from Ephemeris
				double SatClkDrift1 = (double) SatParams1[2];
				// GPS System time at time of transmission
				double t1 = (double) SatParams1[3];
				// ECI coordinates
				double[] ECI1 = (double[]) SatParams1[4];
				SV[0].add(new Satellite(sat1, Arrays.copyOfRange(ECEF_SatClkOff1, 0, 3), ECEF_SatClkOff1[3], t1, tRX,
						SatVel1, SatClkDrift1, ECI1, time));

				double tSV2 = tRX - (sat2.getPseudorange() / SpeedofLight);

				Object[] SatParams2 = ComputeSatPos.computeSatPos(NavMsg, tSV2, tRX, null, ISC2);
				double[] ECEF_SatClkOff2 = (double[]) SatParams2[0];
				double[] SatVel2 = (double[]) SatParams2[1];
				// Note this Clock Drift is derived, it not what we get from Ephemeris
				double SatClkDrift2 = (double) SatParams2[2];
				// GPS System time at time of transmission
				double t2 = (double) SatParams2[3];
				// ECI coordinates
				double[] ECI2 = (double[]) SatParams2[4];

				SV[1].add(new Satellite(sat2, Arrays.copyOfRange(ECEF_SatClkOff2, 0, 3), ECEF_SatClkOff2[3], t2, tRX,
						SatVel2, SatClkDrift2, ECI2, time));

//				double errT = SpeedofLight * Math.abs(t1 - t2);
//				double errECI = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> ECI1[j] - ECI2[j]).map(j -> j * j)
//						.reduce(0, (j, k) -> j + k));
//				if (errECI > 1) {
//					System.out.println();
//				}
//				if (errT > 1) {
//					System.out.println();
//				}

			}
		}
		return SV;
	}
}
