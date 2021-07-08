package com.RINEX_parser.models.IGS;

import java.util.HashMap;

public class IGSOrbit {

	// timestamp of record in GPS time scale
	private double time;
	private HashMap<Character, HashMap<Integer, double[]>> satECEF;

	public IGSOrbit(double time, HashMap<Character, HashMap<Integer, double[]>> satECEF) {
		this.time = time;
		this.satECEF = satECEF;

	}

	public double getTime() {
		return time;
	}

	public HashMap<Character, HashMap<Integer, double[]>> getSatECEF() {
		return satECEF;
	}

}
