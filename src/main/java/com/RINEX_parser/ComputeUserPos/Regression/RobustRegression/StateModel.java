package com.RINEX_parser.ComputeUserPos.Regression.RobustRegression;

import java.util.Arrays;

public class StateModel {

	// ECEF coords and Rcvr Clk in meters
	private double[] xyz;
	private double b;

	public double[] getXYZ() {
		return xyz;
	}

	public double getB() {
		return b;
	}

	public void setXYZ(double[] xyz) {
		int n = xyz.length;
		this.xyz = Arrays.copyOf(xyz, n);

	}

	public void setB(double b) {
		this.b = b;
	}

	public void set(StateModel model) {
		int n = model.getXYZ().length;
		xyz = Arrays.copyOf(model.getXYZ(), n);
		b = model.getB();
	}
}
