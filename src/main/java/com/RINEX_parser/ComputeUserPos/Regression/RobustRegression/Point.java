package com.RINEX_parser.ComputeUserPos.Regression.RobustRegression;

public class Point {

	private double[] h;
	private double y;
	private int index;

	Point(double[] h, double y, int index) {
		this.h = h;
		this.y = y;
		this.index = index;
	}

	public double[] getH() {
		return h;
	}

	public double getY() {
		return y;
	}

	public int getIndex() {
		return index;
	}

}
