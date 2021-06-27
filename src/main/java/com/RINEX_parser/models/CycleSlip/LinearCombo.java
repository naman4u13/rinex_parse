package com.RINEX_parser.models.CycleSlip;

public class LinearCombo {

	private double lc;
	private long t;

	public LinearCombo(double lc, long t) {
		super();
		this.lc = lc;
		this.t = t;
	}

	public double lc() {
		return lc;
	}

	public long t() {
		return t;
	}

}
