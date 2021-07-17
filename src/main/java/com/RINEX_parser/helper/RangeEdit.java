package com.RINEX_parser.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.orekit.models.earth.Geoid;

import com.RINEX_parser.ComputeUserPos.Regression.WLS;
import com.RINEX_parser.fileParser.IONEX;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.LatLonUtil;

public class RangeEdit {
	private static final double SpeedofLight = 299792458;

	public static void RemoveErr(ArrayList<ArrayList<Satellite>> SVlist, double[] PCO, IonoCoeff ionoCoeff, IONEX ionex,
			Geoid geoid, boolean doDopplerSmooth) {

		HashMap<String, ArrayList<Object[]>> satMap = new HashMap<String, ArrayList<Object[]>>();
		for (ArrayList<Satellite> SV : SVlist) {
			Calendar time = SV.get(0).getTime();
			WLS wls = new WLS(SV, PCO, ionoCoeff, time, ionex, geoid);
			double[] refECEF = wls.getTropoCorrECEF();
			double[] refLatLon = ECEFtoLatLon.ecef2lla(refECEF);
			ComputeTropoCorr tropo = new ComputeTropoCorr(refLatLon, time, geoid);

			for (Satellite sat : SV) {
				double[] EleAzm = sat.getElevAzm();
				double tropoErr = tropo.getSlantDelay(EleAzm[0]);

				double ionoErr = 0;

				if (ionex != null) {
					double gcLat = LatLonUtil.gd2gc(refLatLon[0], refLatLon[2]);
					ionoErr = ionex.computeIonoCorr(EleAzm[0], EleAzm[1], gcLat, refLatLon[1], sat.gettRX(),
							sat.getCarrier_frequency(), time);

				} else {
					ionoErr = ComputeIonoCorr.computeIonoCorr(EleAzm[0], EleAzm[1], refLatLon[0], refLatLon[1],
							sat.gettRX(), ionoCoeff, sat.getCarrier_frequency(), time);
					System.err.println("ERROR - Using Klobuchlar model in SF PPP dyanmic kalman computation");
				}
				double corrPR = sat.getPseudorange() + (sat.getSatClkOff() * SpeedofLight) - tropoErr - ionoErr;
				double corrCP = (sat.getCarrier_wavelength() * sat.getCycle()) + (sat.getSatClkOff() * SpeedofLight)
						- tropoErr + ionoErr;
				sat.setPseudorange(corrPR);
				sat.setCycle(corrCP / sat.getCarrier_wavelength());
				if (doDopplerSmooth) {
					String svid = String.valueOf(sat.getSSI()) + String.valueOf(sat.getSVID());
					Object[] data = new Object[] { sat, (sat.getSatClkOff() * SpeedofLight) - tropoErr + ionoErr };
					satMap.computeIfAbsent(svid, k -> new ArrayList<Object[]>()).add(data);
				}

			}
		}
		if (doDopplerSmooth) {
			for (String svid : satMap.keySet()) {
				ArrayList<Object[]> satDataList = satMap.get(svid);

				int index = 0;
				double t0 = ((Satellite) satDataList.get(index)[0]).gettRX();
				double dR = 0;
				ArrayList<Double> PRindex = new ArrayList<Double>();
				PRindex.add(((Satellite) satDataList.get(index)[0]).getPseudorange());
				ArrayList<Double> dRlist = new ArrayList<Double>();
				for (int i = 1; i < satDataList.size(); i++) {

					Satellite sat2 = (Satellite) satDataList.get(i)[0];
					Satellite sat1 = (Satellite) satDataList.get(i - 1)[0];
					double t2 = sat2.gettRX();
					double t1 = sat1.gettRX();
					if (t2 - t0 > 50 || (t2 - t1) > 1.1) {

						double PR = PRindex.stream().mapToDouble(j -> j).average().orElse(0.0);
						((Satellite) satDataList.get(index)[0]).setPseudorange(PR);
						for (int j = index + 1; j < i; j++) {

							((Satellite) satDataList.get(j)[0]).setPseudorange(PR + dRlist.get(j - index - 1));

						}
						dR = 0;
						index = i;
						t0 = ((Satellite) satDataList.get(index)[0]).gettRX();
						PRindex = new ArrayList<Double>();
						PRindex.add(((Satellite) satDataList.get(index)[0]).getPseudorange());
						dRlist = new ArrayList<Double>();
						continue;
					}
					double dCorr = (double) satDataList.get(i)[1] - (double) satDataList.get(i - 1)[1];
					double dr = -sat1.getCarrier_wavelength() * (sat1.getDoppler() + sat2.getDoppler()) * (t2 - t1) / 2;
					dr += dCorr;
					dR += dr;
					dRlist.add(dR);

					PRindex.add(sat2.getPseudorange() - dR);
				}
				double PR = PRindex.stream().mapToDouble(j -> j).average().orElse(0.0);
				((Satellite) satDataList.get(index)[0]).setPseudorange(PR);
				for (int j = index + 1; j < satDataList.size(); j++) {

					((Satellite) satDataList.get(j)[0]).setPseudorange(PR + dRlist.get(j - index - 1));

				}

			}
		}

	}

	public static void alignPhase(ArrayList<ArrayList<Satellite>> SVlist) {
		HashMap<String, Double> map = new HashMap<String, Double>();
		for (ArrayList<Satellite> SV : SVlist) {
			for (Satellite sat : SV) {
				String svid = String.valueOf(sat.getSSI()) + String.valueOf(sat.getSVID());
				double PR = sat.getPseudorange();
				double CP = sat.getCarrier_wavelength() * sat.getCycle();
				if (sat.isLocked()) {
					if (map.containsKey(svid)) {
						CP = CP + map.get(svid);
					} else {
						double delta = PR - CP;
						CP += delta;
						map.put(svid, delta);
					}

				} else {
					if (map.containsKey(svid)) {
						map.remove(svid);
					}
				}
				sat.setCycle(CP / sat.getCarrier_wavelength());

			}
		}
	}

}
