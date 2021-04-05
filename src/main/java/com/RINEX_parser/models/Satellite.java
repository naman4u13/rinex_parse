package com.RINEX_parser.models;

import java.util.Arrays;
import java.util.Calendar;

public class Satellite extends SatelliteModel {

	private double[] ECEF;
	private double SatClkOff;
	// Note this is GPS System time at time of Transmission
	private double t;
	private double[] SatVel;
	// Note this Clock Drift is derived, its not what we get from Ephemeris
	private double SatClkDrift;
	private double[] ECI;
	// Note this is GPS System time at time of Reception + Receiver clock offset
	private double tRX;
	// time
	private Calendar time;

	public double[] getECEF() {
		return ECEF;
	}

	public void setECEF(double[] eCEF) {
		ECEF = eCEF;
	}

	public double[] getECI() {
		return ECI;
	}

	public void setECI(double[] eCI) {
		ECI = eCI;
	}

	public void updateECI(double rcvrClkOff) {
		ECI = new double[3];
		final double OMEGA_E_DOT = 7.2921151467E-5;// WGS-84 value of the Earth's rotation rate

		// eciArg = Earth_Rotation_Rate *(Propgation_Time)
		double eciArg = OMEGA_E_DOT * (tRX - rcvrClkOff - t);
		ECI[0] = (ECEF[0] * Math.cos(eciArg)) + (ECEF[1] * Math.sin(eciArg));
		ECI[1] = -(ECEF[0] * Math.sin(eciArg)) + (ECEF[1] * Math.cos(eciArg));
		ECI[2] = ECEF[2];

	}

	public Satellite(int SVID, double pseudorange, double CNo, double doppler, double[] eCEF, double satClkOff,
			double t, double tRX, double[] satVel, double satClkDrift, double[] ECI, Calendar time) {
		super(SVID, pseudorange, CNo, doppler);
		ECEF = eCEF;
		SatClkOff = satClkOff;
		this.t = t;
		this.tRX = tRX;
		SatVel = satVel;
		SatClkDrift = satClkDrift;
		this.ECI = ECI;
		this.time = time;
	}

	public Satellite(SatelliteModel satModel, double[] eCEF, double satClkOff, double t, double tRX, double[] satVel,
			double satClkDrift, double[] ECI, Calendar time) {
		super(satModel);
		ECEF = eCEF;
		SatClkOff = satClkOff;
		this.t = t;
		this.tRX = tRX;
		SatVel = satVel;
		SatClkDrift = satClkDrift;
		this.ECI = ECI;
		this.time = time;
	}

	public double getSatClkOff() {
		return SatClkOff;
	}

	public void setSatClkOff(double satClkOff) {
		SatClkOff = satClkOff;
	}

	public double getT() {
		return t;
	}

	public void setT(double t) {
		this.t = t;
	}

	@Override
	public String toString() {
		return super.toString() + "Satellite [ECEF=" + Arrays.toString(ECEF) + ", SatClkOff=" + SatClkOff + ", t=" + t
				+ ", tRX=" + tRX + ", SatVel=" + Arrays.toString(SatVel) + ", SatClkDrift=" + SatClkDrift + ", ECI="
				+ Arrays.toString(ECI) + "]";
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

	public double gettRX() {
		return tRX;
	}

	public void settRX(double tRX) {
		this.tRX = tRX;
	}

	public Calendar getTime() {
		return time;
	}

	public void setTime(Calendar time) {
		this.time = time;
	}

}
