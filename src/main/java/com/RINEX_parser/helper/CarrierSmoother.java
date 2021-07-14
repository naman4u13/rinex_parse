package com.RINEX_parser.helper;

import java.util.ArrayList;
import java.util.HashMap;

import org.jfree.ui.RefineryUtilities;

import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.GraphPlotter;

public class CarrierSmoother {

	public static void process(ArrayList<ArrayList<Satellite>> SVlist, boolean useDelta) {

		HashMap<String, ArrayList<Double>> prMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> cpMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, Double> delta = new HashMap<String, Double>();

		for (ArrayList<Satellite> SV : SVlist) {

			for (Satellite sat : SV) {
				String svid = String.valueOf(sat.getSSI()) + String.valueOf(sat.getSVID());
				double pr = sat.getPseudorange();
				double cp = sat.getCarrier_wavelength() * sat.getCycle();
				if (!delta.containsKey(svid)) {
					delta.put(svid, cp - pr);
				}
				if (useDelta) {
					pr = pr + delta.get(svid);
				}
				if (sat.isLocked() && Math.abs(pr - cp) > 5) {
					System.out.println();
				}
				prMap.computeIfAbsent(svid, k -> new ArrayList<Double>()).add(pr);
				cpMap.computeIfAbsent(svid, k -> new ArrayList<Double>()).add(cp);

			}
		}

		for (String svid : prMap.keySet()) {
			GraphPlotter chart = new GraphPlotter("PR-CP - " + svid, "PR-CP - " + svid, prMap.get(svid),
					cpMap.get(svid), svid);

			chart.pack();
			RefineryUtilities.positionFrameRandomly(chart);
			chart.setVisible(true);
		}

	}
}
