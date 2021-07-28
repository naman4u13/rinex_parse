package com.RINEX_parser.helper.CycleSlip;

import java.util.ArrayList;
import java.util.HashMap;

import com.RINEX_parser.models.Satellite;

public class DopplerAidedCS {

	public static HashMap<String, ArrayList<Double>> process(ArrayList<ArrayList<Satellite>> SVlist) {

		double n = 4;
		HashMap<String, ArrayList<Satellite>> satMap = new HashMap<String, ArrayList<Satellite>>();
		for (ArrayList<Satellite> SV : SVlist) {
			for (Satellite sat : SV) {
				String svid = String.valueOf(sat.getSSI()) + String.valueOf(sat.getSVID());
				satMap.computeIfAbsent(svid, k -> new ArrayList<Satellite>()).add(sat);
			}
		}
		HashMap<String, ArrayList<Double>> deltaMap = new HashMap<String, ArrayList<Double>>();
		for (String svid : satMap.keySet()) {
			ArrayList<Satellite> satList = satMap.get(svid);
			for (int i = 1; i < satList.size(); i++) {
				Satellite sat1 = satList.get(i - 1);
				Satellite sat2 = satList.get(i);
				double dT = sat2.gettRX() - sat1.gettRX();
				if (dT > 1.1) {
					continue;
				}
				double phaseDR = sat2.getPhase() - sat1.getPhase();
				double dopplerDR = (sat2.getPseudoRangeRate() + sat1.getPseudoRangeRate()) * dT / 2;
				if (Math.abs(phaseDR - dopplerDR) <= sat2.getCarrier_wavelength() * n) {
					sat2.setLocked(true);
				}
				deltaMap.computeIfAbsent(svid, k -> new ArrayList<Double>()).add(Math.abs(phaseDR - dopplerDR));
			}
		}
		return deltaMap;

	}

}
