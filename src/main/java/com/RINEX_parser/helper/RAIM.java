package com.RINEX_parser.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.ejml.simple.SimpleMatrix;

import com.RINEX_parser.models.Satellite;

/*Work in Progress - not yet ready for the implementation*/
public class RAIM {
	private final static double SpeedofLight = 299792458;

	public static int process2(double[] rxECEF, double rxClkOff, ArrayList<Satellite> SV) {
		int index = -1;

		double alpha = 1e-3;
		int len = SV.size();
		if (len < 5) {
			return index;
		}
		SimpleMatrix r = new SimpleMatrix(len, 1);
		double[][] _H = new double[len][4];
		double[][] _W = new double[len][len];
		double[] res = new double[len];
		for (int i = 0; i < len; i++) {

			Satellite sat = SV.get(i);
			double z = sat.getPseudorange() + (SpeedofLight * sat.getSatClkOff()) - sat.getIonoErr()
					- sat.getTropoErr();
			double z_hat = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> sat.getECI()[j] - rxECEF[j]).map(j -> j * j)
					.reduce(0, (j, k) -> j + k)) + (SpeedofLight * rxClkOff);
			final int _i = i;
			IntStream.range(0, 3).forEach(j -> _H[_i][j] = -(sat.getECI()[j] - rxECEF[j]) / z_hat);
			_H[i][3] = 1;
			res[i] = z - z_hat;
			_W[i][i] = sat.getPrUncM() * sat.getPrUncM();
			r.set(i, (z - z_hat) / sat.getPrUncM());

		}

		int dof = len - 4;
		SimpleMatrix W = new SimpleMatrix(_W);
		SimpleMatrix H = new SimpleMatrix(_H);
		// Sum of Squared Residual
		double ssr = r.transpose().mult(r).get(0);
		double testStat = ssr / dof;
		ChiSquaredDistribution csd = new ChiSquaredDistribution(dof);
		if (csd.cumulativeProbability(testStat) >= (1 - alpha)) {

			NormalDistribution nd = new NormalDistribution();
			double largestStRes = Double.MIN_VALUE;
			double localAlpha = alpha / 2;

			SimpleMatrix Q = W.minus(H.mult((H.transpose().mult(W.invert()).mult(H)).invert()).mult(H.transpose()));

			for (int i = 0; i < len; i++) {

				double q = Math.sqrt(Q.get(i, i));
				double stRes = Math.abs(res[i] / q);

				if (nd.cumulativeProbability(stRes) >= (1 - localAlpha)) {
					if (stRes > largestStRes) {
						index = i;
						largestStRes = stRes;
					}

				}

			}
			if (index != -1) {
				SV.remove(index);
			}
		}
		return index;
	}

	public static ArrayList<Integer> process(double[] rxECEF, double rxClkOff, ArrayList<Satellite> SV) {

		int len = SV.size();
		double thresh = 30;
		double[] res = new double[len];
		double[] _res = new double[len];
		for (int i = 0; i < len; i++) {

			Satellite sat = SV.get(i);
			double z = sat.getPseudorange() - (SpeedofLight * rxClkOff) + (SpeedofLight * sat.getSatClkOff())
					- sat.getIonoErr() - sat.getTropoErr();
			double z_hat = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> sat.getECI()[j] - rxECEF[j]).map(j -> j * j)
					.reduce(0, (j, k) -> j + k));
			res[i] = Math.abs(z - z_hat);
			_res[i] = Math.abs(z - z_hat);

		}

		Arrays.sort(res);
		double median = 0;
		// check for even case
		if (len % 2 != 0) {
			median = res[len / 2];
		} else {
			median = (res[(len - 1) / 2] + res[len / 2]) / 2.0;
		}
		ArrayList<Integer> outlierList = new ArrayList<Integer>();
		for (int i = 0; i < len; i++) {
			if (Math.abs(_res[i] - median) > thresh) {

				outlierList.add(i);
			}
		}
		Collections.sort(outlierList, Collections.reverseOrder());
		for (int i : outlierList) {
			SV.remove(i);
		}
		if (outlierList.size() >= (len / 2)) {
			return new ArrayList<Integer>();
		}
		return outlierList;

	}

}
