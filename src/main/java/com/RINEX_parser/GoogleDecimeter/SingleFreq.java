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
import com.RINEX_parser.models.Observable;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.GoogleDecimeter.AndroidObsv;
import com.RINEX_parser.models.GoogleDecimeter.Derived;
import com.RINEX_parser.utility.Vector;

public class SingleFreq {
	private final static double SpeedofLight = 299792458;

	public static ArrayList<Satellite> process(ObservationMsg obsvMsg, double tRX, double[] userECEF, double cutOffAng,
			double snrMask, HashMap<Long, HashMap<String, HashMap<Integer, Derived>>> derivedMap,
			HashMap<Long, HashMap<String, HashMap<Integer, AndroidObsv>>> gnssLogMap, Calendar time,
			String[] obsvCodeList, long weekNo, boolean useIGS, Orbit orbit, Clock clock, Antenna antenna) {

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY kk:mm:ss.SSS");
		String errStr = sdf.format(time.getTime());
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		ArrayList<Satellite> SV = new ArrayList<Satellite>();
		long key = Math.round(tRX * 1e3);
		HashMap<String, HashMap<Integer, Derived>> derObsvMap = null;
		HashMap<String, HashMap<Integer, AndroidObsv>> logObsvMap = null;
		if (derivedMap.containsKey(key)) {
			derObsvMap = derivedMap.get(key);
		} else if (derivedMap.containsKey(key + 1)) {
			derObsvMap = derivedMap.get(key + 1);
		} else if (derivedMap.containsKey(key - 1)) {
			derObsvMap = derivedMap.get(key - 1);
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

		if (gnssLogMap.containsKey(key)) {
			logObsvMap = gnssLogMap.get(key);
		} else if (derivedMap.containsKey(key + 1)) {
			logObsvMap = gnssLogMap.get(key + 1);
		} else if (derivedMap.containsKey(key - 1)) {
			logObsvMap = gnssLogMap.get(key - 1);
		} else {
			System.err.println("Error in GNSS LOG file");
			return null;
		}

		for (String obsvCode : obsvCodeList) {

			if (!derObsvMap.containsKey(obsvCode)) {

				System.err.println("No data for obsvCode " + obsvCode + " in derived.csv at time = " + errStr);
				continue;
			}

			HashMap<Integer, Derived> navMap = derObsvMap.get(obsvCode);
			HashMap<Integer, AndroidObsv> gnssLog = logObsvMap.get(obsvCode);
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

					double tSV = tRX - (sat.getPseudorange() / SpeedofLight);

					double satClkOff = clock.getBias(tSV, svid, obsvCode, true);
					// GPS System transmission time
					double t = tSV - satClkOff;
					double[][] satPV = orbit.getPV(t, svid, polyOrder, SSI);
					if (satPV == null) {
						System.err.println(SSI + "" + svid + " MGEX data absent");
						continue;
					}
					double[] satECEF = satPV[0];
					double[] satVel = satPV[1];
					double relativistic_error = -2 * (Vector.dotProd(satECEF, satVel)) / Math.pow(SpeedofLight, 2);
					// Correct sat clock offset for relativistic error and recompute the Sat coords
					satClkOff += relativistic_error;
					t = tSV - satClkOff;

//					double[] satPC_windup = antenna.getSatPC_windup(SVID, obsvCode, tRX, weekNo, satECEF, userECEF);
//					IntStream.range(0, 3).forEach(j -> satECEF[j] = satPC_windup[j]);
					double[] EleAzm = ComputeEleAzm.computeEleAzm(userECEF, satECEF);
					sat.setPseudorange(navData.getRawPrM() - navData.getIsrbM());
					sat.setPrUncM(navData.getRawPrUncM());
					_sat = new Satellite(sat, satECEF, satClkOff, t, tRX, satVel, 0.0, null, EleAzm, time);

				} else {
					double[] satECEF = navData.getSatECEF();
					double[] ElevAzm = ComputeEleAzm.computeEleAzm(userECEF, Arrays.copyOfRange(satECEF, 0, 3));
					double t = (navData.gettSV() * 1e-9) - navData.getSatClkBias();
					sat.setPseudorange(navData.getRawPrM() - navData.getIsrbM());
					sat.setPrUncM(navData.getRawPrUncM());
					// NOTE: satClkDrift require investigation
					_sat = new Satellite(sat, satECEF, navData.getSatClkBias(), t, tRX, navData.getSatVel(),
							navData.getSatClkDrift(), null, ElevAzm, time);

				}
				_sat.setGnssLog(gnssLog.get(svid));

				if (_sat.getPrUncM() >= 150 || _sat.getGnssLog().getBiasUnc() >= 1e-3) {

					continue;
				}

				_sat.compECI();
				_sat.setDerived(navData);

				SV.add(_sat);

			}
		}

		if (cutOffAng >= 0) {
			SV.removeIf(i -> i.getElevAzm()[0] < Math.toRadians(cutOffAng));
		}
		if (snrMask >= 0) {
			SV.removeIf(i -> i.getCNo() < snrMask);
		}

		return SV;

	}
}
