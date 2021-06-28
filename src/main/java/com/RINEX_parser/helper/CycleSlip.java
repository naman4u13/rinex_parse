package com.RINEX_parser.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.CycleSlip.LinearCombo;
import com.RINEX_parser.models.CycleSlip.MWfilter;
import com.RINEX_parser.models.CycleSlip.Outlier;
import com.RINEX_parser.utility.Combination;

public class CycleSlip {
	// factor
	private int nL;
	private double threshold;
	private int samplingRate;
	private static final double SpeedofLight = 299792458;

	public CycleSlip(int samplingRate) {
		this.samplingRate = 30;
	}

	public HashMap<Integer, int[]> process_1s(ArrayList<ArrayList<Satellite>[]> dualSVlist) {

		// Maximum Threshold for GF
		double a0 = 0.08;
		// Max arc length
		int maxAL = 10;
		// Max arc length
		int minAL = 10;
		// iono time correlation
		double T0 = 60;
		// factor
		nL = 2;
		// maximum period of time allowed without declaring cycle slip
		double maxDGper = 40;

		threshold = a0 / (1 + Math.pow(Math.E, -maxDGper / T0));

		HashMap<Integer, List<LinearCombo>> map = new HashMap<Integer, List<LinearCombo>>();
		HashMap<Integer, int[]> res = new HashMap<Integer, int[]>();
		HashMap<Integer, Outlier> outlier = new HashMap<Integer, Outlier>();
		int n = dualSVlist.size();

		for (int i = 0; i < n; i++) {
			HashMap<Integer, Outlier> cOutlier = new HashMap<Integer, Outlier>();
			ArrayList<Satellite>[] SV = dualSVlist.get(i);
			long t = SV[0].get(0).gettRX();
			if (i == 508) {
				System.out.print("");
			}
			for (int j = 0; j < SV[0].size(); j++) {
				Satellite sat1 = SV[0].get(j);
				Satellite sat2 = SV[1].get(j);
				double cp1 = sat1.getCycle() * sat1.getCarrier_wavelength();
				double cp2 = sat2.getCycle() * sat2.getCarrier_wavelength();
				int SVID = sat1.getSVID();

				double gf = cp1 - cp2;
				List<LinearCombo> gfList = map.getOrDefault(SVID, null);
				boolean isSlip = true;
				if (SVID == 4) {
					System.out.print("");
				}
				if (gfList == null) {
					gfList = new ArrayList<LinearCombo>();
				} else {
					if (t - gfList.get(gfList.size() - 1).t() > maxDGper) {
						gfList = new ArrayList<LinearCombo>();
						if (outlier.containsKey(SVID)) {
							Outlier info = outlier.get(SVID);
							if (t - info.t() < maxDGper) {
								gfList.add(new LinearCombo(info.gf(), info.t()));
							}
						}
					} else {
						if (gfList.size() >= 3) {
							if (gfList.size() >= maxAL) {
								isSlip = detect(gfList.subList(gfList.size() - maxAL, gfList.size()), gf, t, SVID);

							} else if (gfList.size() > 3) {
								isSlip = detect(gfList.subList(0, gfList.size()), gf, t, SVID);

							}

							else {
								isSlip = detect(gfList.subList(0, 3), gf, t, SVID);

							}
							if (isSlip) {
								if (outlier.containsKey(SVID)) {
									if (gfList.size() > 3) {
										gfList = new ArrayList<LinearCombo>();

									} else {
										gfList = gfList.subList(1, 3);
									}
									gfList.add(new LinearCombo(outlier.get(SVID).gf(), outlier.get(SVID).t()));

								}
								cOutlier.put(SVID, new Outlier(i, j, gf, t));
							} else {
								if (outlier.containsKey(SVID)) {
									Outlier info = outlier.get(SVID);
									dualSVlist.get(info.i())[0].remove(info.j());
									dualSVlist.get(info.i())[1].remove(info.j());
								}
							}

						}
					}
				}
				if (!cOutlier.containsKey(SVID)) {
					gfList.add(new LinearCombo(gf, t));
				} else {
					System.out.print("");

				}

				map.put(SVID, gfList);
				SV[0].get(j).setLocked(!isSlip);
				SV[1].get(j).setLocked(!isSlip);

				res.computeIfAbsent(SVID, k -> new int[n])[i] = isSlip ? 1 : 2;

				if (SVID == 4) {
					System.out.print("");
				}
			}
			outlier.clear();
			outlier.putAll(cOutlier);
		}
		return res;
	}

	// Optimal for 30s sample rate
	public HashMap<Integer, int[]> process(ArrayList<ArrayList<Satellite>[]> dualSVlist) {

		// GF CSD based variables
		// Maximum Threshold for GF
		double a0 = 0.08;
		// Max arc length for GF CSD
		int maxAL_GF = 10;
		// iono time correlation
		double T0 = 60;
		// factor
		nL = 2;
		// maximum period of time allowed without declaring cycle slip
		double maxDGper = 40;

		// MW CSD based variables
		// Min arc length for MW CSD
		int minAL_MW = 5;
		int kFact = 5;

		threshold = a0 / (1 + Math.pow(Math.E, -maxDGper / T0));

		HashMap<Integer, List<LinearCombo>> gfMap = new HashMap<Integer, List<LinearCombo>>();
		HashMap<Integer, MWfilter> mwMap = new HashMap<Integer, MWfilter>();
		HashMap<Integer, int[]> res = new HashMap<Integer, int[]>();

		int n = dualSVlist.size();

		for (int i = 0; i < n; i++) {

			ArrayList<Satellite>[] SV = dualSVlist.get(i);
			long t = SV[0].get(0).gettRX();

			for (int j = 0; j < SV[0].size(); j++) {
				Satellite sat1 = SV[0].get(j);
				Satellite sat2 = SV[1].get(j);
				double cp1 = sat1.getCycle() * sat1.getCarrier_wavelength();
				double cp2 = sat2.getCycle() * sat2.getCarrier_wavelength();
				int SVID = sat1.getSVID();

				double gf = Combination.GeometryFree(cp1, cp2);
				List<LinearCombo> gfList = gfMap.getOrDefault(SVID, null);
				boolean isGFslip = true;

				if (gfList == null) {
					gfList = new ArrayList<LinearCombo>();
				} else {
					if (t - gfList.get(gfList.size() - 1).t() > maxDGper) {
						gfList = new ArrayList<LinearCombo>();

					} else {
						if (gfList.size() >= 3) {
							if (gfList.size() >= maxAL_GF) {
								isGFslip = detect(gfList.subList(gfList.size() - maxAL_GF, gfList.size()), gf, t, SVID);

							} else if (gfList.size() > 3) {
								isGFslip = detect(gfList.subList(0, gfList.size()), gf, t, SVID);

							}

							else {
								isGFslip = detect(gfList.subList(0, 3), gf, t, SVID);

							}
							if (isGFslip) {

								if (gfList.size() > 3) {
									gfList = new ArrayList<LinearCombo>();

								} else {
									gfList = gfList.subList(1, 3);
								}

							}
						}

					}
				}

				gfList.add(new LinearCombo(gf, t));
				gfMap.put(SVID, gfList);

				// Melbourne-Wubenna based CS detector
				double pr1 = sat1.getPseudorange();
				double pr2 = sat2.getPseudorange();
				double f1 = sat1.getCarrier_frequency();
				double f2 = sat2.getCarrier_frequency();
				double mw = Combination.MelbourneWubenna(pr1, pr2, cp1, cp2, f1, f2);
				double lamW = SpeedofLight / (f1 - f2);
				MWfilter mwObj = mwMap.getOrDefault(SVID, null);
				boolean isMWslip = true;

				if (mwObj == null) {
					mwObj = new MWfilter(samplingRate);
				} else {
					List<LinearCombo> mwList = mwObj.getMwList();
					if (t - mwList.get(mwList.size() - 1).t() > maxDGper) {
						mwObj = new MWfilter(samplingRate);

					} else {
						if (mwList.size() >= minAL_MW) {

							double d = mw - mwObj.getMean();
							double d300 = mw - mwObj.getMean300();
							double sigma = Math.sqrt(mwObj.getSigmaSq());
							double th = kFact * sigma;
							isMWslip = (Math.abs(d) > th) && (sigma <= lamW) && (Math.abs(d300) > lamW);
							if (isMWslip) {
								mwObj = new MWfilter(samplingRate);
							}
						}
					}

				}
				mwObj.update(mw, t);
				mwMap.put(SVID, mwObj);

				boolean isSlip = isGFslip || isMWslip;
				SV[0].get(j).setLocked(!isSlip);
				SV[1].get(j).setLocked(!isSlip);
				if (isMWslip == true && isGFslip == false && gfList.size() > 5) {
					System.out.print("");
				}
				res.computeIfAbsent(SVID, k -> new int[n])[i] = isSlip ? 1 : 2;

			}

		}
		return res;

	}

	private boolean detect(List<LinearCombo> list, double meas, double t, int SVID) {
		int len = list.size();
		double[][] _H = new double[len][3];
		double[][] _Y = new double[len][1];
		for (int i = 0; i < len; i++) {
			LinearCombo ele = list.get(i);
			int x = (int) (((ele.t() - list.get(0).t()) / samplingRate) + 1);
			_H[i][0] = x * x;
			_H[i][1] = x;
			_H[i][2] = 1;
			_Y[i][0] = ele.lc();
		}
		SimpleMatrix H = new SimpleMatrix(_H);
		SimpleMatrix Y = new SimpleMatrix(_Y);

		SimpleSVD<SimpleMatrix> svd = new SimpleSVD<SimpleMatrix>(H.getMatrix(), true);

		double[] singularVal = svd.getSingularValues();
		int n = singularVal.length;
		double eps = Math.ulp(1.0);
		double[][] _Wplus = new double[n][n];
		double maxW = Double.MIN_VALUE;
		for (int i = 0; i < n; i++) {
			maxW = Math.max(singularVal[i], maxW);
		}
		double tolerance = Math.max(H.numRows(), H.numCols()) * eps * maxW;
		for (int i = 0; i < n; i++) {
			double val = singularVal[i];
			if (val < tolerance) {
				continue;
			}
			_Wplus[i][i] = 1 / val;
		}
		SimpleMatrix Wplus = new SimpleMatrix(_Wplus);
		Wplus = Wplus.transpose();
		SimpleMatrix U = svd.getU();
		SimpleMatrix V = svd.getV();
		SimpleMatrix Ut = U.transpose();
		SimpleMatrix X = V.mult(Wplus).mult(Ut).mult(Y);
		double a = X.get(0);
		double b = X.get(1);
		double c = X.get(2);

		SimpleMatrix Ye = H.mult(X);
		SimpleMatrix res = Ye.minus(Y);
		res = res.elementMult(res);
		double rms = 0;
		for (int i = 0; i < len; i++) {
			rms += res.get(i);
		}
		rms = Math.sqrt(rms);

		int x = (int) ((t - list.get(0).t()) / samplingRate) + 1;
		double pred = (a * Math.pow(x, 2)) + (b * (x)) + c;
		double abs = Math.abs(meas - pred);
		boolean isSlip = (abs > threshold) && (abs > (nL * rms));
		if ((abs > (nL * rms) == false) && ((abs > threshold) == true)) {
			System.out.print("");
		}
		return isSlip;
	}
}
