package com.RINEX_parser.helper;

import java.util.ArrayList;
import java.util.HashMap;

import org.jfree.ui.RefineryUtilities;

import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.GraphPlotter;

public class Analyzer {

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

	public static void android(ArrayList<ArrayList<Satellite>> SVlist, String mobName) {
		HashMap<String, ArrayList<Double>> prUncMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> prRateUncMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> biasUncMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> adrUncMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> prRateMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> adrMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> prMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> hardClkDiscCountMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Satellite>> satMap = new HashMap<String, ArrayList<Satellite>>();
		for (ArrayList<Satellite> SV : SVlist) {

			for (Satellite sat : SV) {
				String svid = String.valueOf(sat.getSSI()) + String.valueOf(sat.getSVID());
				prUncMap.computeIfAbsent(svid, k -> new ArrayList<Double>()).add(sat.getPrUncM());
				prRateUncMap.computeIfAbsent(svid, k -> new ArrayList<Double>())
						.add(sat.getGnssLog().getPrRateUncMps());
				biasUncMap.computeIfAbsent(svid, k -> new ArrayList<Double>()).add(sat.getGnssLog().getBiasUnc());
				adrUncMap.computeIfAbsent(svid, k -> new ArrayList<Double>()).add(sat.getGnssLog().getAdrUncM());
				prRateMap.computeIfAbsent(svid, k -> new ArrayList<Double>()).add(sat.getPseudoRangeRate());
				adrMap.computeIfAbsent(svid, k -> new ArrayList<Double>()).add(sat.getPhase());
				prMap.computeIfAbsent(svid, k -> new ArrayList<Double>()).add(sat.getPseudorange());
				hardClkDiscCountMap.computeIfAbsent(svid, k -> new ArrayList<Double>())
						.add((double) sat.getGnssLog().getHardClkDiscCount());
				satMap.computeIfAbsent(svid, k -> new ArrayList<Satellite>()).add(sat);
			}
		}
		for (String svid : prUncMap.keySet()) {

			GraphPlotter[] chartList = new GraphPlotter[8];
			chartList[0] = new GraphPlotter(mobName + " PseudorangeUnc", "PseudorangeUnc - " + svid, prUncMap.get(svid),
					svid);
			chartList[1] = new GraphPlotter(mobName + " PseudorangeRateUnc", "PseudorangeRateUnc - " + svid,
					prRateUncMap.get(svid), svid);
			chartList[2] = new GraphPlotter(mobName + " BiasUnc", "BiasUnc - " + svid, biasUncMap.get(svid), svid);
			chartList[3] = new GraphPlotter(mobName + " ADRUnc", "ADRUnc - " + svid, adrUncMap.get(svid), svid);
			chartList[4] = new GraphPlotter(mobName + " PRrate", "PRrate - " + svid, prRateMap.get(svid), svid);
			chartList[5] = new GraphPlotter(mobName + " ADR", "ADR - " + svid, adrMap.get(svid), svid);
			chartList[6] = new GraphPlotter(mobName + " PR", "PR - " + svid, prMap.get(svid), svid);
			chartList[7] = new GraphPlotter(mobName + " HardClkDiscCount", "HardClkDiscCount - " + svid,
					hardClkDiscCountMap.get(svid), svid);
			for (GraphPlotter chart : chartList) {
				chart.pack();
				RefineryUtilities.positionFrameRandomly(chart);
				chart.setVisible(true);
			}
		}
	}
}
