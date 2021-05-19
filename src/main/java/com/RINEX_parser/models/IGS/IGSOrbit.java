package com.RINEX_parser.models.IGS;

import java.util.HashMap;

public class IGSOrbit {

	// timestamp of record in GPS time scale
	private long time;
	private HashMap<Integer, double[]> satECEF;

	public IGSOrbit(long time, HashMap<Integer, double[]> satECEF) {
		this.time = time;
		this.satECEF = satECEF;

	}

	public long getTime() {
		return time;
	}

	public HashMap<Integer, double[]> getSatECEF() {
		return satECEF;
	}

}
