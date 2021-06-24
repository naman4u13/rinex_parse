package com.RINEX_parser.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

import com.RINEX_parser.models.Satellite;

public class CycleSlip {

	private static double threshold;

	public static HashMap<Integer, int[]> process(ArrayList<ArrayList<Satellite>[]> dualSVlist) {

		double wl1 = dualSVlist.get(0)[0].get(0).getCarrier_wavelength();

		double wl2 = dualSVlist.get(0)[1].get(0).getCarrier_wavelength();
		threshold = wl2 - wl1;
		int winSize = 10;
		HashMap<Integer, List<Double>> map = new HashMap<Integer, List<Double>>();
		HashMap<Integer, int[]> res = new HashMap<Integer, int[]>();
		int n = dualSVlist.size();
		for (int i = 0; i < n; i++) {

			ArrayList<Satellite>[] SV = dualSVlist.get(i);
			HashMap<Integer, List<Double>> cMap = new HashMap<Integer, List<Double>>();
			for (int j = 0; j < SV[0].size(); j++) {
				Satellite sat1 = SV[0].get(j);
				Satellite sat2 = SV[1].get(j);
				double cp1 = sat1.getCycle() * wl1;
				double cp2 = sat2.getCycle() * wl2;
				int SVID = sat1.getSVID();

				double gf = cp1 - cp2;
				List<Double> gfList = map.getOrDefault(SVID, null);
				boolean isSlip = true;

				if (gfList == null) {
					gfList = new ArrayList<Double>();
				} else {
					if (gfList.size() >= 3) {
						if (gfList.size() >= winSize) {
							isSlip = detect(gfList.subList(gfList.size() - winSize, gfList.size()), gf, SVID);
							if (isSlip) {
								gfList = new ArrayList<Double>();
							}
						} else if (gfList.size() > 3) {
							isSlip = detect(gfList.subList(0, gfList.size()), gf, SVID);
							if (isSlip) {
								gfList = new ArrayList<Double>();
							}
						}

						else {
							isSlip = detect(gfList.subList(0, 3), gf, SVID);
							if (isSlip) {
								gfList = gfList.subList(1, 3);
							}
						}

					}
				}

				gfList.add(gf);
				cMap.put(SVID, gfList);
				SV[0].get(j).setLocked(!isSlip);
				SV[1].get(j).setLocked(!isSlip);

				res.computeIfAbsent(SVID, k -> new int[n])[i] = isSlip ? 1 : 2;
			}
			map.clear();
			map.putAll(cMap);
		}
		return res;
	}

	private static boolean detect(List<Double> list, double meas, int SVID) {
		int len = list.size();
		double[][] _H = new double[len][3];
		double[][] _Y = new double[len][1];
		for (int i = 0; i < len; i++) {
			int x = i + 1;
			_H[i][0] = x * x;
			_H[i][1] = x;
			_H[i][2] = 1;
			_Y[i][0] = list.get(i);
		}
		SimpleMatrix H = new SimpleMatrix(_H);
		SimpleMatrix Y = new SimpleMatrix(_Y);

		SimpleSVD<SimpleMatrix> svd = new SimpleSVD<SimpleMatrix>(H.getMatrix(), true);
		int rank = svd.rank();
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

		double pred = (a * Math.pow(len + 1, 2)) + (b * (len + 1)) + c;
		boolean isSlip = Math.abs(meas - pred) > threshold;

		return isSlip;
	}

}
