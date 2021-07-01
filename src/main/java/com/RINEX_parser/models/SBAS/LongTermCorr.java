package com.RINEX_parser.models.SBAS;

public class LongTermCorr extends SbasRoot {

	private int velCode;
	// deltaX, deltaY, deltaZ and deltaClkOff
	private double[] deltaX;
	// deltaXrate, deltaYrate, deltaZrate and deltaClkDrift
	private double[] deltaXrate;

	public LongTermCorr(double GPSTime, long weekNo, int velCode, double[] deltaX) {
		super(GPSTime, weekNo);
		// TODO Auto-generated constructor stub
		this.velCode = velCode;
		this.deltaX = deltaX;

	}

	public LongTermCorr(double GPSTime, long weekNo, int velCode, double[] deltaX, double[] deltaXrate) {
		super(GPSTime, weekNo);
		// TODO Auto-generated constructor stub
		this.velCode = velCode;
		this.deltaX = deltaX;
		this.deltaXrate = deltaXrate;

	}

	public int getVelCode() {
		return velCode;
	}

	public void setVelCode(int velCode) {
		this.velCode = velCode;
	}

	public double[] getDeltaX() {
		return deltaX;
	}

	public void setDeltaX(double[] deltaX) {
		this.deltaX = deltaX;
	}

	public double[] getDeltaXrate() {
		return deltaXrate;
	}

	public void setDeltaXrate(double[] deltaXrate) {
		this.deltaXrate = deltaXrate;
	}

	public double getToA() {
		return getGPStime();
	}
}
