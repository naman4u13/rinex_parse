package com.RINEX_parser.ComputeUserPos.KalmanFilter.Models;

import java.util.ArrayList;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;

import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.Matrix;

public class KFconfig extends KF {

	private final double SpeedofLight = 299792458;
	private final double c2 = SpeedofLight * SpeedofLight;
	// Typical Allan Variance Coefficients for TCXO (low quality)
	private final double h0 = 2E-19;
	private final double h_2 = 2E-20;
	private final double sf = h0 / 2;
	private final double sg = 2 * Math.PI * Math.PI * h_2;

	public void configStaticSPP(double deltaT) {

		double[][] F = new double[5][5];
		double[][] Q = new double[5][5];
		Q[3][3] = ((sf * deltaT) + ((sg * Math.pow(deltaT, 3)) / 3)) * c2;
		Q[3][4] = (sg * Math.pow(deltaT, 2)) * c2 / 2;
		Q[4][3] = (sg * Math.pow(deltaT, 2)) * c2 / 2;
		Q[4][4] = sg * deltaT * c2;

		IntStream.range(0, 5).forEach(x -> F[x][x] = 1);
		F[3][4] = deltaT;
		super.configure(F, Q);
	}

	public void configDynamicSPP(double deltaT) {

		double[][] F = new double[4][4];
		double[][] Q = new double[4][4];
		IntStream.range(0, 3).forEach(i -> {
			Q[i][i] = 1e8;
			F[i][i] = 1;
		});
		Q[3][3] = 9e10;
		F[3][3] = 1;
		super.configure(F, Q);
	}

	public void configurePPP(double deltaT, int SVcount, boolean isStatic) {

		int n = SVcount + 5;// Rcvr pos(3), clk off(1),Residual wet tropo(1), Ambiguity
							// params(satCount)
		double[][] F = new double[n][n];
		double[][] Q = new double[n][n];
		Q[3][3] = 9e10;
		Q[4][4] = (0.0001) * (deltaT / (60 * 60));
		IntStream.range(0, n).forEach(x -> F[x][x] = 1);
		if (!isStatic) {

			IntStream.range(0, 3).forEach(x -> Q[x][x] = 1e8);
		}

		super.configure(F, Q);
	}

	public void configurePhaseConn(ArrayList<Double> _dR, ArrayList<Double> _dRcov, ArrayList<double[]> h,
			ArrayList<Satellite> SV) {

		int n = SV.size();
		SimpleMatrix dR = new SimpleMatrix(n, 1, false, _dR.stream().mapToDouble(i -> i).toArray());
		SimpleMatrix dRcov = new SimpleMatrix(n, n);

		double[][] _H = new double[n][];
		for (int i = 0; i < n; i++) {
			dRcov.set(i, i, _dRcov.get(i));
			_H[i] = h.get(i);

		}
		SimpleMatrix H = new SimpleMatrix(_H);
		SimpleMatrix Hinv = null;
		try {
			Hinv = Matrix.getPseudoInverse(H);
		} catch (Exception e) {
			// TODO: handle exception
			System.err.println();
		}
		SimpleMatrix deltaX = Hinv.mult(dR);
		SimpleMatrix F = new SimpleMatrix(8, 8);
		IntStream.range(0, 8).forEach(i -> F.set(i, i, 1));
		SimpleMatrix Q = Hinv.mult(dRcov).mult(Hinv.transpose());
		// predict State
		setState(getState().plus(deltaX));
		super.configure(F, Q);

	}
}
