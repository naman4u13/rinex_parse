package com.RINEX_parser.models.CycleSlip;

public class Outlier {

	private int epochIndex;
	private int satIndex;
	private double gf;
	private double t;

	public Outlier(int epochIndex, int satIndex, double gf, double t) {
		super();
		this.epochIndex = epochIndex;
		this.satIndex = satIndex;
		this.gf = gf;
		this.t = t;
	}

	public int i() {
		return epochIndex;

	}

	public int j() {
		return satIndex;

	}

	public double gf() {
		return gf;
	}

	public double t() {
		return t;
	}

}
