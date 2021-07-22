package com.RINEX_parser.helper;

import java.util.ArrayList;
import java.util.HashMap;

import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.Interpolator;

public class RangeEdit {
	private static final double SpeedofLight = 299792458;

	public static void RemoveErr(ArrayList<ArrayList<Satellite>> SVlist, boolean doDopplerSmooth,
			boolean doCarrierSmooth) {

		HashMap<String, ArrayList<Satellite>> satMap = new HashMap<String, ArrayList<Satellite>>();

		for (ArrayList<Satellite> SV : SVlist) {

			for (Satellite sat : SV) {
				if (!(sat.hasIonoErr() && sat.hasTropoErr())) {
					System.err.println("Satellite does not have errors");
					return;
				}

				double ionoErr = sat.getIonoErr();
				double tropoErr = sat.getTropoErr();

				double corrPR = sat.getPseudorange() + (sat.getSatClkOff() * SpeedofLight) - tropoErr - ionoErr;
				double corrCP = (sat.getPhase()) + (sat.getSatClkOff() * SpeedofLight) - tropoErr + ionoErr;
				sat.setPseudorange(corrPR);
				sat.setPhase(corrCP);
				if (doDopplerSmooth || doCarrierSmooth) {
					String svid = String.valueOf(sat.getSSI()) + String.valueOf(sat.getSVID());

					satMap.computeIfAbsent(svid, k -> new ArrayList<Satellite>()).add(sat);
				}

			}
		}

		if (doDopplerSmooth && doCarrierSmooth) {
			System.err.println("FATAL ERROR: both doppler and carrier smoothing cannot be enabled");
			return;
		}

		if (doDopplerSmooth) {
			dopplerCorrect(satMap);
			for (String svid : satMap.keySet()) {
				ArrayList<Satellite> satList = satMap.get(svid);

				int index = 0;
				double t0 = satList.get(index).gettRX();
				double dR = 0;
				ArrayList<Double> PRindex = new ArrayList<Double>();
				PRindex.add(satList.get(index).getPseudorange());
				ArrayList<Double> dRlist = new ArrayList<Double>();
				int n = satList.size();
				for (int i = 1; i < n; i++) {

					Satellite sat2 = satList.get(i);
					Satellite sat1 = satList.get(i - 1);
					double t2 = sat2.gettRX();
					double t1 = sat1.gettRX();
					if (t2 - t0 > 20 || (t2 - t1) > 1.1 || !sat2.isDopplerValid()) {

						double PR = PRindex.stream().mapToDouble(j -> j).average().orElse(0.0);
						satList.get(index).setPseudorange(PR);
						for (int j = index + 1; j < i; j++) {

							satList.get(j).setPseudorange(PR + dRlist.get(j - index - 1));
						}
						dR = 0;
						PRindex = new ArrayList<Double>();
						dRlist = new ArrayList<Double>();
						if (!sat2.isDopplerValid()) {

							while (!satList.get(i).isDopplerValid()) {
								if (i + 1 >= n) {
									break;
								} else {
									i++;
									continue;
								}
							}
							if (i + 1 >= n) {
								index = i;
								if (i == n - 1) {
									PRindex.add(satList.get(index).getPseudorange());
								}
								continue;
							}

						}
						index = i;
						t0 = satList.get(index).gettRX();

						PRindex.add(satList.get(index).getPseudorange());

						continue;
					}
					double corr1 = (sat1.getSatClkOff() * SpeedofLight) - sat1.getTropoErr() + sat1.getIonoErr();
					double corr2 = (sat2.getSatClkOff() * SpeedofLight) - sat2.getTropoErr() + sat2.getIonoErr();
					double dCorr = corr2 - corr1;
					double dr = (sat1.getPseudoRangeRate() + sat2.getPseudoRangeRate()) * (t2 - t1) / 2;
					dr += dCorr;
					dR += dr;
					dRlist.add(dR);

					PRindex.add(sat2.getPseudorange() - dR);
				}
				double PR = PRindex.stream().mapToDouble(j -> j).average().orElse(0.0);
				if (PR == 0) {
					System.err.println("PR in doppler smoothing is assigned wrong");
					return;
				}
				satList.get(index).setPseudorange(PR);
				for (int j = index + 1; j < n; j++) {

					satList.get(j).setPseudorange(PR + dRlist.get(j - index - 1));

				}

			}
		}
		if (doCarrierSmooth) {

			for (String svid : satMap.keySet()) {
				ArrayList<Satellite> satList = satMap.get(svid);

				int index = 0;
				double t0 = satList.get(index).gettRX();
				double dR = 0;
				ArrayList<Double> PRindex = new ArrayList<Double>();
				PRindex.add(satList.get(index).getPseudorange());
				ArrayList<Double> dRlist = new ArrayList<Double>();
				int n = satList.size();
				for (int i = 1; i < n; i++) {

					Satellite sat2 = satList.get(i);
					Satellite sat1 = satList.get(i - 1);
					double t = sat2.gettRX();

					if (t - t0 > 500 || !sat2.isLocked()) {

						double PR = PRindex.stream().mapToDouble(j -> j).average().orElse(0.0);
						satList.get(index).setPseudorange(PR);
						for (int j = index + 1; j < i; j++) {

							satList.get(j).setPseudorange(PR + dRlist.get(j - index - 1));
						}
						dR = 0;
						PRindex = new ArrayList<Double>();
						dRlist = new ArrayList<Double>();
						if (!sat2.isLocked()) {

							while (!satList.get(i).isLocked()) {
								if (i + 1 >= n) {
									break;
								} else {
									i++;
									continue;
								}
							}
							if (i + 1 >= n) {
								index = i;
								if (i == n - 1) {
									PRindex.add(satList.get(index).getPseudorange());
								}
								continue;
							}

						}
						index = i;
						t0 = satList.get(index).gettRX();

						PRindex.add(satList.get(index).getPseudorange());

						continue;
					}

					double dr = sat2.getPhase() - sat1.getPhase();
					dR += dr;
					dRlist.add(dR);

					PRindex.add(sat2.getPseudorange() - dR);
				}
				double PR = PRindex.stream().mapToDouble(j -> j).average().orElse(0.0);
				if (PR == 0) {
					System.err.println("PR in Carrier smoothing is assigned wrong");
					return;
				}
				satList.get(index).setPseudorange(PR);
				for (int j = index + 1; j < n; j++) {

					satList.get(j).setPseudorange(PR + dRlist.get(j - index - 1));

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
				double CP = sat.getPhase();
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
				sat.setPhase(CP);

			}
		}
	}

	private static void dopplerCorrect(HashMap<String, ArrayList<Satellite>> satMap) {
		HashMap<String, ArrayList<Double>> resMap = new HashMap<String, ArrayList<Double>>();
		double thresh = 1e5;
		for (String svid : satMap.keySet()) {
			ArrayList<Satellite> satList = satMap.get(svid);
			double[] prRate = satList.stream().mapToDouble(i -> i.getPseudoRangeRate()).toArray();
			int n = satList.size();
			// Not enough satellites
			if (n < 11) {
				continue;
			}
			for (int i = 0; i < n; i++) {
				double[] X = new double[10];
				double[] Y = new double[10];
				int start = 0;
				int end = 0;
				double x = satList.get(i).gettRX();
				if (i >= 5 && i <= n - 6) {

					start = i - 5;
					end = i + 6;

				} else if (i < 5) {
					start = 0;
					end = 11;

				} else {
					start = n - 11;
					end = n;

				}
				int index = 0;
				for (int j = start; j < end; j++) {
					if (j == i) {
						continue;
					}
					try {
						X[index] = satList.get(j).gettRX();
					} catch (Exception e) {
						// TODO: handle exception
						System.err.println();
					}
					Y[index] = prRate[j];
					index++;
				}
				double y = Interpolator.lagrange(X, Y, x, false)[0];
				// resMap.computeIfAbsent(svid, k -> new ArrayList<Double>()).add(Math.abs(y -
				// prRate[i]));
				if (Math.abs(y - prRate[i]) > thresh) {
					satList.get(i).setDopplerValid(false);

				}
			}
		}

//		for (String SVID : resMap.keySet()) {
//			GraphPlotter chart = new GraphPlotter(" Res" + SVID, "Res - " + SVID, resMap.get(SVID), SVID);
//			chart.pack();
//			RefineryUtilities.positionFrameRandomly(chart);
//			chart.setVisible(true);
//		}
	}

	private static void dopplerSmoothHatch(HashMap<String, ArrayList<Satellite>> satMap) {
		for (String svid : satMap.keySet()) {
			ArrayList<Satellite> satList = satMap.get(svid);

		}

	}

}
