package com.RINEX_parser.models;

import java.util.Arrays;
import java.util.Calendar;

import com.RINEX_parser.models.GoogleDecimeter.AndroidObsv;

public class Satellite extends Observable {

	private double[] ECEF;
	private double satClkOff;
	// Note this is GPS System time at time of Transmission
	private double t;
	private double[] satVel;
	// Note this Clock Drift is derived, its not what we get from Ephemeris
	private double satClkDrift;
	private double[] ECI;
	// Note this is GPS System time at time of Reception + Receiver clock offset
	private double tRX;
	// time
	private Calendar time;
	// Elevation and Azimuth Angle
	private double[] ElevAzm;
	private double phaseWindUp;
	private AndroidObsv gnssLog;

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
		compECI(rcvrClkOff);

	}

	public void compECI() {
		compECI(0);
	}

	public void compECI(double time) {
		ECI = new double[3];
		final double OMEGA_E_DOT = 7.2921151467E-5;// WGS-84 value of the Earth's rotation rate

		// eciArg = Earth_Rotation_Rate *(Propgation_Time)
		double eciArg = OMEGA_E_DOT * (tRX - time - t);
		ECI[0] = (ECEF[0] * Math.cos(eciArg)) + (ECEF[1] * Math.sin(eciArg));
		ECI[1] = -(ECEF[0] * Math.sin(eciArg)) + (ECEF[1] * Math.cos(eciArg));
		ECI[2] = ECEF[2];

	}

	public Satellite(Observable satModel, double[] eCEF, double satClkOff, double t, double tRX, double[] satVel,
			double satClkDrift, double[] ECI, double[] ElevAzm, Calendar time) {
		super(satModel);
		ECEF = eCEF;
		this.satClkOff = satClkOff;
		this.t = t;
		this.tRX = tRX;
		this.satVel = satVel;
		this.satClkDrift = satClkDrift;
		this.ECI = ECI;
		this.time = time;
		this.ElevAzm = ElevAzm;
	}

	public double getSatClkOff() {
		return satClkOff;
	}

	public void setSatClkOff(double satClkOff) {
		this.satClkOff = satClkOff;
	}

	public double getT() {
		return t;
	}

	public void setT(double t) {
		this.t = t;
	}

	@Override
	public String toString() {
		return super.toString() + "Satellite [ECEF=" + Arrays.toString(ECEF) + ", satClkOff=" + satClkOff + ", t=" + t
				+ ", tRX=" + tRX + ", satVel=" + Arrays.toString(satVel) + ", satClkDrift=" + satClkDrift + ", ECI="
				+ Arrays.toString(ECI) + "]";
	}

	public double[] getSatVel() {
		return satVel;
	}

	public void setSatVel(double[] satVel) {
		this.satVel = satVel;
	}

	public double getSatClkDrift() {
		return satClkDrift;
	}

	public void setSatClkDrift(double satClkDrift) {
		this.satClkDrift = satClkDrift;
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

	public double[] getElevAzm() {
		return ElevAzm;
	}

	public void setElevAzm(double[] elevAzm) {
		ElevAzm = elevAzm;
	}

	public double getPhaseWindUp() {
		return phaseWindUp;
	}

	public void setPhaseWindUp(double phaseWindUp) {
		this.phaseWindUp = phaseWindUp;
	}

	public AndroidObsv getGnssLog() {
		return gnssLog;
	}

	public void setGnssLog(AndroidObsv gnssLog) {
		this.gnssLog = gnssLog;
	}

}
