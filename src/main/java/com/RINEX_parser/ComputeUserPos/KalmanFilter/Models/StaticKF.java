package com.RINEX_parser.ComputeUserPos.KalmanFilter.Models;

import java.util.stream.IntStream;

public class StaticKF extends KF {

	private final double SpeedofLight = 299792458;
	private final double c2 = SpeedofLight * SpeedofLight;
	// Typical Allan Variance Coefficients for TCXO (low quality)
	private final double h0 = 2E-19;
	private final double h_2 = 2E-20;
	private final double sf = h0 / 2;
	private final double sg = 2 * Math.PI * Math.PI * h_2;

	public void predict(double deltaT, double[][] unitLOS) {
		double[][] Q = new double[5][5];

		Q[3][3] = (sf * deltaT) + ((sg * Math.pow(deltaT, 3)) / 3);
		Q[3][4] = (sg * Math.pow(deltaT, 2)) / 2;
		Q[4][3] = (sg * Math.pow(deltaT, 2)) / 2;
		Q[4][4] = sg * deltaT;

		double[][] F = new double[5][5];
		IntStream.range(0, 5).forEach(x -> F[x][x] = 1);
		F[3][4] = deltaT;

		int SVcount = unitLOS.length;

		// H is the Jacobian matrix of partial derivatives Observation Model(h) of with
		// respect to x
		double[][] H = new double[SVcount][5];
		IntStream.range(0, SVcount).forEach(x -> {
			IntStream.range(0, 3).forEach(y -> H[x][y] = -unitLOS[x][y]);
			H[x][3] = 1;
		});

		configure(F, Q, H);
		predict();
	}
}
