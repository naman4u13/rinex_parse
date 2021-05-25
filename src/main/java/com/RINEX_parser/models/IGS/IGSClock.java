package com.RINEX_parser.models.IGS;

import java.util.HashMap;

public class IGSClock {
	// timestamp of record in GPS time scale
	private long time;

	private HashMap<Integer, Double> clkBias;

	public IGSClock(long time, HashMap<Integer, Double> clkBias) {
		this.time = time;
		this.clkBias = clkBias;
	}

	public long getTime() {
		return time;
	}

	public HashMap<Integer, Double> getClkBias() {
		return clkBias;
	}

}
