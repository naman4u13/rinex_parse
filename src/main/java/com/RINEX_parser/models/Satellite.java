package com.RINEX_parser.models;

import java.util.Arrays;

public class Satellite extends SatelliteModel {

	private double[] ECEF;
	private double SatClkOff;
	// Note this Satellite signal transmission time including the bias
	private double tSV;

	public Satellite(int SVID, double pseudorange, double CNo, double[] eCEF, double satClkOff, double tSV) {
		super(SVID, pseudorange, CNo);
		ECEF = eCEF;
		SatClkOff = satClkOff;
		this.tSV = tSV;
	}

	public double[] getECEF() {
		return ECEF;
	}

	public void setECEF(double[] eCEF) {
		ECEF = eCEF;
	}

	public double getSatClkOff() {
		return SatClkOff;
	}

	public void setSatClkOff(double satClkOff) {
		SatClkOff = satClkOff;
	}

	public double gettSV() {
		return tSV;
	}

	public void settSV(double tSV) {
		this.tSV = tSV;
	}

	@Override
	public String toString() {
		return super.toString() + "Satellite [ECEF=" + Arrays.toString(ECEF) + ", SatClkOff=" + SatClkOff + ", tSV="
				+ tSV + "]";
	}

}
