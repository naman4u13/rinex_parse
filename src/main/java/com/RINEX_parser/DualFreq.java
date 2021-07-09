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
import com.RINEX_parser.helper.ComputeEleAzm;
import com.RINEX_parser.helper.ComputeSatPos;
import com.RINEX_parser.models.NavigationMsg;
import com.RINEX_parser.models.Observable;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.Closest;
import com.RINEX_parser.utility.Vector;

public class DualFreq {

	final static double SpeedofLight = 299792458;

	public static ArrayList<Satellite>[] process(ObservationMsg obsvMsg,
			HashMap<Integer, ArrayList<NavigationMsg>> NavMsgs, String[] obsvCode, boolean useIGS, boolean useBias,
			Bias bias, Orbit orbit, Clock clock, Antenna antenna, double tRX, long weekNo, double[] userECEF,
			Calendar time, double cutOffAng) {
		ArrayList<Satellite>[] SV = new ArrayList[2];
		SV[0] = new ArrayList<Satellite>();
		SV[1] = new ArrayList<Satellite>();
		ArrayList<Observable> observables1 = obsvMsg.getObsvSat(obsvCode[0]);
		ArrayList<Observable> observables2 = obsvMsg.getObsvSat(obsvCode[1]);
		if (observables1 == null || observables1 == null) {
			return SV;
		}
		int satCount = observables1.size();

		if (useIGS) {
			char SSI = obsvCode[0].charAt(0);
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

				double[] satClkOff = clock.getBias(tSV1, SVID, obsvCode, true);

				// GPS System transmission time
				double t = tSV1 - satClkOff[0];

				double[][] satPV = orbit.getPV(t, SVID, polyOrder, SSI);
				double[] satECEF_MC = satPV[0];
				double[] satVel = satPV[1];

				double relativistic_error = -2 * (Vector.dotProd(satECEF_MC, satVel)) / Math.pow(SpeedofLight, 2);
				// Correct sat clock offset for relativistic error and recompute the Sat coords
				satClkOff[0] += relativistic_error;
				satClkOff[1] += relativistic_error;
				t = tSV1 - satClkOff[0];

				double[][] satPC_windup = antenna.getSatPC_windup(SVID, obsvCode, tRX, weekNo, satECEF_MC, userECEF);
				int fN = obsvCode.length;
				double[][] satECEF_PC = new double[fN][3];
				double[] windup = new double[fN];
				for (int j = 0; j < fN; j++) {
					for (int k = 0; k < 3; k++) {
						satECEF_PC[j][k] = satPC_windup[j][k];
					}
					// fractional Wind up in cycles, will require further processing to correct for
					// full cycles and then multiply by wavelength
					// The cycle value will be same for both frequencies
					windup[j] = satPC_windup[j][3];
				}
				double[] EleAzm = ComputeEleAzm.computeEleAzm(userECEF, satECEF_PC[0]);

				Satellite _sat1 = new Satellite(sat1, satECEF_PC[0], satClkOff[0], t, tRX, satVel, 0.0, null, EleAzm,
						time);
				_sat1.compECI();
				_sat1.setPhaseWindUp(windup[0]);
				Satellite _sat2 = new Satellite(sat2, satECEF_PC[1], satClkOff[1], t, tRX, satVel, 0.0, null, EleAzm,
						time);
				_sat2.compECI();
				_sat2.setPhaseWindUp(windup[1]);

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
				ArrayList<Double> TOCs = (ArrayList<Double>) navMsg.stream().map(j -> j.getTOC())
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
				if (SVID != sat2.getSVID()) {
					System.err.println("SVID didn't match FATAL ERROR");
				}
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
				double[] EleAzm = ComputeEleAzm.computeEleAzm(userECEF, Arrays.copyOfRange(ECEF_SatClkOff1, 0, 3));
				SV[0].add(new Satellite(sat1, Arrays.copyOfRange(ECEF_SatClkOff1, 0, 3), ECEF_SatClkOff1[3], t1, tRX,
						SatVel1, SatClkDrift1, ECI1, EleAzm, time));

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
						SatVel2, SatClkDrift2, ECI2, EleAzm, time));

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
		if (cutOffAng >= 0) {
			SV[0].removeIf(i -> i.getElevAzm()[0] < Math.toRadians(cutOffAng));
			SV[1].removeIf(i -> i.getElevAzm()[0] < Math.toRadians(cutOffAng));

		}

		return SV;
	}
}
