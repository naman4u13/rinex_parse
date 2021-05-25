package com.RINEX_parser.utility;

import java.util.stream.IntStream;

public class Interpolator {

	public static double[] lagrange(double[] X, double[] Y, double x, boolean getDeriv) {
		int n = X.length;
		double num = IntStream.range(0, n).mapToDouble(i -> x - X[i]).reduce(1, (i, j) -> i * j);
		double[] y = new double[] { 0, 0 };
		for (int i = 0; i < n; i++) {
			double coeff = num / (x - X[i]);
			int _i = i;
			double denom = IntStream.range(0, i).mapToDouble(j -> X[_i] - X[j]).reduce(1, (j, k) -> j * k);
			denom *= IntStream.range(i + 1, n).mapToDouble(j -> X[_i] - X[j]).reduce(1, (j, k) -> j * k);
			coeff = coeff / denom;
			y[0] += coeff * Y[i];
			double derCoeff = 0;
			if (getDeriv) {
				for (int j = 0; j < n; j++) {
					derCoeff += 1 / (x - X[j]);
				}
				derCoeff = derCoeff - (1 / (x - X[i]));
				derCoeff *= coeff;
			}

			y[1] += derCoeff * Y[i];

		}

		return y;
	}

	public static double linear(double[] X, double[] Y, double x) {
		double y = Y[0] + (x - X[0]) * ((Y[1] - Y[0]) / (X[1] - X[0]));
		return y;
	}

}
