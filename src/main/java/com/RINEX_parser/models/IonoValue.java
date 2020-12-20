package com.RINEX_parser.models;

import java.util.Date;

public class IonoValue {

	private Date time;
	private double ionoCorr;
	int SVID;

	public IonoValue(Date time, double ionoCorr, int SVID) {
		super();
		this.SVID = SVID;
		this.time = time;
		this.ionoCorr = ionoCorr;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public double getIonoCorr() {
		return ionoCorr;
	}

	public void setIonoCorr(double ionoCorr) {
		this.ionoCorr = ionoCorr;
	}

	public int getSVID() {
		return SVID;
	}

	public void setSVID(int sVID) {
		SVID = sVID;
	}

}
