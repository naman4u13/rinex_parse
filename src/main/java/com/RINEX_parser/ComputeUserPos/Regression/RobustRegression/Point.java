package com.RINEX_parser.ComputeUserPos.Regression.RobustRegression;

import com.RINEX_parser.models.Satellite;

public class Point {

	private Satellite sat;

	Point(Satellite sat) {
		this.sat = sat;
	}

	public Satellite getSat() {
		return sat;
	}

}
