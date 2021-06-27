package com.RINEX_parser.utility;

public class Combination {

	public static double GeometryFree(double o1, double o2) {
		return o1 - o2;
	}

	public static double IonoFree(double o1, double o2, double f1, double f2) {
		double IF = ((Math.pow(f1, 2) * o1) - (Math.pow(f2, 2) * o2)) / (Math.pow(f1, 2) - Math.pow(f2, 2));
		return IF;
	}

	public static double WideLane(double o1, double o2, double f1, double f2) {
		double WL = ((f1 * o1) - (f2 * o2)) / (f1 - f2);
		return WL;
	}

	public static double NarrowLane(double o1, double o2, double f1, double f2) {
		double NL = ((f1 * o1) + (f2 * o2)) / (f1 + f2);
		return NL;
	}

	public static double MelbourneWubenna(double pr1, double pr2, double cp1, double cp2, double f1, double f2) {
		double cp_WL = WideLane(cp1, cp2, f1, f2);
		double pr_NL = NarrowLane(pr1, pr2, f1, f2);
		return cp_WL - pr_NL;
	}

}
