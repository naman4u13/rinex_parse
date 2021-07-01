package com.RINEX_parser.models.CycleSlip;

public class LinearCombo {

	private double lc;
	private double t;

	public LinearCombo(double lc, double t) {
		super();
		this.lc = lc;
		this.t = t;
	}

	public double lc() {
		return lc;
	}

	public double t() {
		return t;
	}

}
