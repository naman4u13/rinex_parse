package com.RINEX_parser.models.IGS;

import java.util.HashMap;

public class IGSClock {
	// timestamp of record in GPS time scale
	private double time;

	private HashMap<Character, HashMap<Integer, Double>> clkBias;

	public IGSClock(double time, HashMap<Character, HashMap<Integer, Double>> clkBias) {
		this.time = time;
		this.clkBias = clkBias;
	}

	public double getTime() {
		return time;
	}

	public HashMap<Character, HashMap<Integer, Double>> getClkBias() {
		return clkBias;
	}

}
