package com.RINEX_parser.ComputeUserPos.KalmanFilter.Models;

import java.util.ArrayList;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;

import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.SatUtil;

public class KF {

	private final double SpeedofLight = 299792458;
	private final double c2 = SpeedofLight * SpeedofLight;
	// kinematics description
	private SimpleMatrix F, Q;

	// sytem state estimate
	private SimpleMatrix x, P;

	public void configure(double[][] F, double[][] Q) {
		this.F = new SimpleMatrix(F);
		this.Q = new SimpleMatrix(Q).scale(c2);

	}

	public void setState(double[][] x, double[][] P) {
		this.x = new SimpleMatrix(x);
		this.P = new SimpleMatrix(P);
	}

	public void setState(SimpleMatrix x, SimpleMatrix P) {
		this.x = x;
		this.P = P;
	}

	public void predict() {
		// x = F x
		x = F.mult(x);

		// P = F P F' + Q
		P = F.mult(P).mult(F.transpose()).plus(Q);

	}

	public void update(double[][] _z, double[][] _R, double[][] _ze, double[][] _H) {

		SimpleMatrix z = new SimpleMatrix(_z);
		SimpleMatrix R = new SimpleMatrix(_R);
		SimpleMatrix ze = new SimpleMatrix(_ze);
		SimpleMatrix H = new SimpleMatrix(_H);
		SimpleMatrix Ht = H.transpose();
		// Kalman Gain
		SimpleMatrix K = P.mult(Ht).mult(((H.mult(P).mult(Ht)).plus(R)).invert());

		// Posterior State Estimate
		x = x.plus((K.mult(z.minus(ze))));
		SimpleMatrix KH = K.mult(H);
		SimpleMatrix I = SimpleMatrix.identity(KH.numRows());
		// Posterior Estimate Error
		// Joseph Form to ensure Positive Definiteness
		// P = (I-KH)P(I-KH)' + KRK'
		P = ((I.minus(KH)).mult(P).mult((I.minus(KH)).transpose())).plus(K.mult(R).mult(K.transpose()));

	}

	// Iterated Kalman Filter
	public void update(double[][] _z, double[][] _R, double[][] _ze, ArrayList<Satellite> SV, double[] PCO) {
		SimpleMatrix z = new SimpleMatrix(_z);
		SimpleMatrix R = new SimpleMatrix(_R);
		SimpleMatrix ze = new SimpleMatrix(_ze);
		SimpleMatrix xPrior = x;

		SimpleMatrix H = null;
		SimpleMatrix K = null;

		int SVcount = SV.size();
		for (int i = 0; i < 5; i++) {

			double[] estECEF = new double[] { x.get(0) + PCO[0], x.get(1) + PCO[1], x.get(2) + PCO[2], x.get(3) };
			double[][] unitLOS = SatUtil.getUnitLOS(SV, estECEF);
			// H is the Jacobian matrix of partial derivatives Observation Model(h) of with
			// respect to x
			double[][] _H = new double[SVcount][5];
			IntStream.range(0, SVcount).forEach(j -> {
				IntStream.range(0, 3).forEach(k -> _H[j][k] = -unitLOS[j][k]);
				_H[j][3] = 1;
			});
			H = new SimpleMatrix(_H);
			SimpleMatrix Ht = H.transpose();
			// Kalman Gain

			K = P.mult(Ht).mult(((H.mult(P).mult(Ht)).plus(R)).invert());

			// Posterior State Estimate
			x = xPrior.plus((K.mult(z.minus(ze))));

		}
		SimpleMatrix KH = K.mult(H);
		SimpleMatrix I = SimpleMatrix.identity(KH.numRows());
		// Posterior Estimate Error
		// Joseph Form to ensure Positive Definiteness
		// P = (I-KH)P(I-KH)' + KRK'
		P = ((I.minus(KH)).mult(P).mult((I.minus(KH)).transpose())).plus(K.mult(R).mult(K.transpose()));

	}

	public SimpleMatrix getState() {
		return x;
	}

	public SimpleMatrix getCovariance() {
		return P;
	}
}
