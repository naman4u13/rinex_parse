package com.RINEX_parser.models;

public class IonoCoeff {

	private double alpha0;
	private double alpha1;
	private double alpha2;
	private double alpha3;
	private double beta0;
	private double beta1;
	private double beta2;
	private double beta3;

	public void setGPSA(double[] GPSA) {
		alpha0 = GPSA[0];
		alpha1 = GPSA[1];
		alpha2 = GPSA[2];
		alpha3 = GPSA[3];
	}

	public void setGPSB(double[] GPSB) {
		beta0 = GPSB[0];
		beta1 = GPSB[1];
		beta2 = GPSB[2];
		beta3 = GPSB[3];
	}

	@Override
	public String toString() {
		return "IonoCoeff [alpha0=" + alpha0 + ", alpha1=" + alpha1 + ", alpha2=" + alpha2 + ", alpha3=" + alpha3
				+ ", beta0=" + beta0 + ", beta1=" + beta1 + ", beta2=" + beta2 + ", beta3=" + beta3 + "]";
	}

	public double getAlpha0() {
		return alpha0;
	}

	public double getAlpha1() {
		return alpha1;
	}

	public double getAlpha2() {
		return alpha2;
	}

	public double getAlpha3() {
		return alpha3;
	}

	public double getBeta0() {
		return beta0;
	}

	public double getBeta1() {
		return beta1;
	}

	public double getBeta2() {
		return beta2;
	}

	public double getBeta3() {
		return beta3;
	}

}
