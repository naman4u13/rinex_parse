package com.RINEX_parser.GoogleDecimeter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.TimeZone;

import com.RINEX_parser.helper.ComputeEleAzm;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Observable;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.GoogleDecimeter.Derived;

public class SingleFreq {
	private final static double SpeedofLight = 299792458;

	public static ArrayList<Satellite> process(ObservationMsg obsvMsg, IonoCoeff ionoCoeff, double tRX,
			double[] userECEF, boolean useCutOffAng,
			HashMap<Long, HashMap<String, HashMap<Integer, Derived>>> derivedMap, Calendar time, String[] obsvCodeList,
			long weekNo) {

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY kk:mm:ss.SSS");
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
			String errStr = sdf.format(time.getTime());

			ArrayList<Observable> x = null;
			for (int i = 0; i < obsvCodeList.length; i++) {
				if (obsvMsg.getObsvSat(obsvCodeList[i]) != null) {
					x = obsvMsg.getObsvSat(obsvCodeList[i]);
					break;
				}
			}

			if (x == null) {
				System.err.println("No PR info available or captured = " + errStr);
				return SV;
			}
			x.removeAll(Collections.singleton(null));
			if (x.size() == 0) {
				System.err.println("No PR info available or captured = " + errStr);
				return SV;
			}

			System.err.println("Missing data in derived.csv at time = " + errStr);
			long tSV = Math.round(((tRX - (x.get(0).getPseudorange() / SpeedofLight)) + (weekNo * 604800)) * 1e9);
			long _tRX = Math.round((tRX + (604800 * weekNo)) * 1e3);

			return SV;
		}

		for (String obsvCode : obsvCodeList) {

			if (!obsvMap.containsKey(obsvCode)) {
				String errStr = sdf.format(time.getTime());
				System.err.println("No data for obsvCode " + obsvCode + " in derived.csv at time = " + errStr);
				continue;
			}

			HashMap<Integer, Derived> navMap = obsvMap.get(obsvCode);
			ArrayList<Observable> observables = obsvMsg.getObsvSat(obsvCode);
			if (observables == null) {
				continue;
			}
			observables.removeAll(Collections.singleton(null));
			int satCount = observables.size();
			for (int i = 0; i < satCount; i++) {
				Observable sat = observables.get(i);
				int svid = sat.getSVID();
				if (!navMap.containsKey(svid)) {
					String errStr = sdf.format(time.getTime());
					System.err.println("No data for svid " + svid + " belonging to obsvCode" + obsvCode
							+ " in derived.csv at time = " + errStr);
					continue;
				}
				Derived navData = navMap.get(svid);

				double[] satECEF = navData.getSatECEF();
				double[] ElevAzm = ComputeEleAzm.computeEleAzm(userECEF, Arrays.copyOfRange(satECEF, 0, 3));
				double t = (navData.gettSV() * 1e-9) - navData.getSatClkBias();

				sat.setPseudorange(sat.getPseudorange() - navData.getIsrbM());
				// NOTE: satClkDrift require investigation
				Satellite _sat = new Satellite(sat, satECEF, navData.getSatClkBias(), t, tRX, navData.getSatVel(),
						navData.getSatClkDrift(), null, ElevAzm, time);
				_sat.compECI();
				SV.add(_sat);

			}
		}

		if (useCutOffAng) {
			SV.removeIf(i -> i.getElevAzm()[0] < Math.toRadians(0));
		}

		return SV;

	}
}
