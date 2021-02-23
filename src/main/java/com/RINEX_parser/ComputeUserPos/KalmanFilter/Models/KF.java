package com.RINEX_parser.ComputeUserPos.KalmanFilter.Models;

import org.ejml.simple.SimpleMatrix;

public class KF {

	private final double SpeedofLight = 299792458;
	private final double c2 = SpeedofLight * SpeedofLight;
	// kinematics description
	private SimpleMatrix F, Q, H;

	// sytem state estimate
	private SimpleMatrix x, P;

	public void configure(double[][] F, double[][] Q, double[][] H) {
		this.F = new SimpleMatrix(F);
		this.Q = new SimpleMatrix(Q).scale(c2);
		this.H = new SimpleMatrix(H);
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
		System.out.println();
	}

	public void update(double[][] _z, double[][] _R, double[][] _ze) {

		SimpleMatrix z = new SimpleMatrix(_z);
		SimpleMatrix R = new SimpleMatrix(_R);
		SimpleMatrix ze = new SimpleMatrix(_ze);

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
		System.out.println();
	}

	public SimpleMatrix getState() {
		return x;
	}

	public SimpleMatrix getCovariance() {
		return P;
	}
}
