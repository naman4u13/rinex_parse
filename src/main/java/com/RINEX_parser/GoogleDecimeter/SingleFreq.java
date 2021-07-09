package com.RINEX_parser.GoogleDecimeter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.TimeZone;

import com.RINEX_parser.fileParser.Antenna;
import com.RINEX_parser.fileParser.Clock;
import com.RINEX_parser.fileParser.Orbit;
import com.RINEX_parser.helper.ComputeEleAzm;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Observable;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.GoogleDecimeter.Derived;
import com.RINEX_parser.utility.Vector;

public class SingleFreq {
	private final static double SpeedofLight = 299792458;

	public static ArrayList<Satellite> process(ObservationMsg obsvMsg, IonoCoeff ionoCoeff, double tRX,
			double[] userECEF, double cutOffAng, HashMap<Long, HashMap<String, HashMap<Integer, Derived>>> derivedMap,
			Calendar time, String[] obsvCodeList, long weekNo, boolean useIGS, Orbit orbit, Clock clock,
			Antenna antenna) {

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY kk:mm:ss.SSS");
		String errStr = sdf.format(time.getTime());
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		ArrayList<Satellite> SV = new ArrayList<Satellite>();
		long key = Math.round(tRX * 1e3);
		HashMap<String, HashMap<Integer, Derived>> obsvMap = null;

		if (derivedMap.containsKey(key)) {
			obsvMap = derivedMap.get(key);
		} else if (derivedMap.containsKey(key + 1)) {
			obsvMap = derivedMap.get(key + 1);
		} else if (derivedMap.containsKey(key - 1)) {
			obsvMap = derivedMap.get(key - 1);
		} else {

			ArrayList<Observable> errObsv = null;
			for (int i = 0; i < obsvCodeList.length; i++) {
				if (obsvMsg.getObsvSat(obsvCodeList[i]) != null) {
					errObsv = obsvMsg.getObsvSat(obsvCodeList[i]);
					break;
				}
			}

			if (errObsv == null) {
				System.err.println("No PR info available or captured = " + errStr);
				return SV;
			}
			errObsv.removeAll(Collections.singleton(null));
			if (errObsv.size() == 0) {
				System.err.println("No PR info available or captured = " + errStr);
				return SV;
			}

			System.err.println("Missing data in derived.csv at time = " + errStr);
			long tSV = Math.round(((tRX - (errObsv.get(0).getPseudorange() / SpeedofLight)) + (weekNo * 604800)) * 1e9);
			long _tRX = Math.round((tRX + (604800 * weekNo)) * 1e3);

			return SV;
		}

		for (String obsvCode : obsvCodeList) {

			if (!obsvMap.containsKey(obsvCode)) {

				System.err.println("No data for obsvCode " + obsvCode + " in derived.csv at time = " + errStr);
				continue;
			}

			HashMap<Integer, Derived> navMap = obsvMap.get(obsvCode);
			ArrayList<Observable> observables = obsvMsg.getObsvSat(obsvCode);
			if (observables == null) {

				System.err.println("No data for obsvCode " + obsvCode + " in Rinex Obs at time = " + errStr);
				continue;
			}
			observables.removeAll(Collections.singleton(null));
			char SSI = obsvCode.charAt(0);
			int polyOrder = 10;
			int satCount = observables.size();
			if (useIGS) {

				orbit.findPts(tRX, polyOrder);
				clock.findPts(tRX);
			}
			for (int i = 0; i < satCount; i++) {
				Observable sat = observables.get(i);
				int svid = sat.getSVID();
				if (!navMap.containsKey(svid)) {

					System.err.println("No data for svid " + svid + " belonging to obsvCode" + obsvCode
							+ " in derived.csv at time = " + errStr);
					continue;
				}
				Satellite _sat = null;
				Derived navData = navMap.get(svid);
				if (useIGS) {
					int SVID = sat.getSVID();
					double tSV = tRX - (sat.getPseudorange() / SpeedofLight);

					double satClkOff = clock.getBias(tSV, SVID, obsvCode, true);
					// GPS System transmission time
					double t = tSV - satClkOff;
					double[][] satPV = orbit.getPV(t, SVID, polyOrder, SSI);
					double[] satECEF = satPV[0];
					double[] satVel = satPV[1];
					double relativistic_error = -2 * (Vector.dotProd(satECEF, satVel)) / Math.pow(SpeedofLight, 2);
					// Correct sat clock offset for relativistic error and recompute the Sat coords
					satClkOff += relativistic_error;
					t = tSV - satClkOff;

//					double[] satPC_windup = antenna.getSatPC_windup(SVID, obsvCode, tRX, weekNo, satECEF, userECEF);
//					IntStream.range(0, 3).forEach(j -> satECEF[j] = satPC_windup[j]);
					double[] EleAzm = ComputeEleAzm.computeEleAzm(userECEF, satECEF);
					sat.setPseudorange(sat.getPseudorange() - navData.getIsrbM());
					_sat = new Satellite(sat, satECEF, satClkOff, t, tRX, satVel, 0.0, null, EleAzm, time);

				} else {
					double[] satECEF = navData.getSatECEF();
					double[] ElevAzm = ComputeEleAzm.computeEleAzm(userECEF, Arrays.copyOfRange(satECEF, 0, 3));
					double t = (navData.gettSV() * 1e-9) - navData.getSatClkBias();

					sat.setPseudorange(sat.getPseudorange() - navData.getIsrbM());
					// NOTE: satClkDrift require investigation
					_sat = new Satellite(sat, satECEF, navData.getSatClkBias(), t, tRX, navData.getSatVel(),
							navData.getSatClkDrift(), null, ElevAzm, time);
				}
				_sat.compECI();
				SV.add(_sat);

			}
		}

		if (cutOffAng >= 0) {
			SV.removeIf(i -> i.getElevAzm()[0] < Math.toRadians(cutOffAng));
		}

		return SV;

	}
}
