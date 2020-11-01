package com.RINEX_parser.models;

import java.util.Arrays;

public class Satellite extends SatelliteModel {

	private double[] ECEF;
	private double SatClkOff;

	public Satellite(int SVID, double pseudorange, double[] eCEF, double satClkOff) {
		super(SVID, pseudorange);
		ECEF = eCEF;
		SatClkOff = satClkOff;
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

	@Override
	public String toString() {
		return super.toString() + " Satellite [ECEF=" + Arrays.toString(ECEF) + ", SatClkOff=" + SatClkOff + "]";
	}

}
