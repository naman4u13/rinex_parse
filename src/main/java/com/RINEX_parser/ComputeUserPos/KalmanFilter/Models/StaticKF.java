package com.RINEX_parser.ComputeUserPos.KalmanFilter.Models;

import java.util.ArrayList;
import java.util.stream.IntStream;

import com.RINEX_parser.models.Satellite;

public class StaticKF extends KF {

	private final double SpeedofLight = 299792458;
	private final double c2 = SpeedofLight * SpeedofLight;
	// Typical Allan Variance Coefficients for TCXO (low quality)
	private final double h0 = 2E-19;
	private final double h_2 = 2E-20;
	private final double sf = h0 / 2;
	private final double sg = 2 * Math.PI * Math.PI * h_2;

	public void configureSPP(double deltaT) {

		double[][] F = new double[5][5];
		double[][] Q = new double[5][5];
		Q[3][3] = (sf * deltaT) + ((sg * Math.pow(deltaT, 3)) / 3);
		Q[3][4] = (sg * Math.pow(deltaT, 2)) / 2;
		Q[4][3] = (sg * Math.pow(deltaT, 2)) / 2;
		Q[4][4] = sg * deltaT;

		IntStream.range(0, 5).forEach(x -> F[x][x] = 1);
		F[3][4] = deltaT;
		super.configure(F, Q);
	}

	public void configurePPP(double deltaT, ArrayList<Satellite>[] SV) {

		int SVcount = SV[0].size();
		int n = SVcount + 5;// Rcvr pos(3), clk off(1),Residual wet tropo(1), Ambiguity
							// params(satCount)
		double[][] F = new double[n][n];
		double[][] Q = new double[n][n];
		Q[3][3] = 9e10 / c2;

		Q[4][4] = (0.0001 / c2) * (deltaT / (60 * 60));

		IntStream.range(0, n).forEach(x -> F[x][x] = 1);
		F[3][3] = 0;
		super.configure(F, Q);
	}
}
