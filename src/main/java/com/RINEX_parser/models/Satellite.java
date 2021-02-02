package com.RINEX_parser.models;

import java.util.Arrays;

public class Satellite extends SatelliteModel {

	private double[] ECEF;
	private double SatClkOff;
	// Note this Satellite signal transmission time including the bias
	private double tSV;
	private double[] SatVel;
	// Note this Clock Drift is derived, its not what we get from Ephemeris
	private double SatClkDrift;

	public double[] getECEF() {
		return ECEF;
	}

	public void setECEF(double[] eCEF) {
		ECEF = eCEF;
	}

	public Satellite(int SVID, double pseudorange, double CNo, double doppler, double[] eCEF, double satClkOff,
			double tSV, double[] satVel, double satClkDrift) {
		super(SVID, pseudorange, CNo, doppler);
		ECEF = eCEF;
		SatClkOff = satClkOff;
		this.tSV = tSV;
		SatVel = satVel;
		SatClkDrift = satClkDrift;
	}

	public Satellite(SatelliteModel satModel, double[] eCEF, double satClkOff, double tSV, double[] satVel,
			double satClkDrift) {
		super(satModel);
		ECEF = eCEF;
		SatClkOff = satClkOff;
		this.tSV = tSV;
		SatVel = satVel;
		SatClkDrift = satClkDrift;
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
				+ tSV + ", SatVel=" + Arrays.toString(SatVel) + ", SatClkDrift=" + SatClkDrift + "]";
	}

	public double[] getSatVel() {
		return SatVel;
	}

	public void setSatVel(double[] satVel) {
		SatVel = satVel;
	}

	public double getSatClkDrift() {
		return SatClkDrift;
	}

	public void setSatClkDrift(double satClkDrift) {
		SatClkDrift = satClkDrift;
	}

}
