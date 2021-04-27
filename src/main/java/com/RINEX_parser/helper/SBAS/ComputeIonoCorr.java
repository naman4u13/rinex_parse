package com.RINEX_parser.helper.SBAS;

import java.util.HashMap;

enum Flag {
	VIABLE, UNVIABLE, TERMINATE;
}

public class ComputeIonoCorr {

	private final static double Earth_Radius = 6378136.3;// in meter
	private final static double maxED_Height = 350000;// Height of Max Electron Density in meter
	private final static double SpeedofLight = 299792458;

	public static double computeIonoCorr(double ElevAng_rad, double AzmAng_rad, double userLat_deg, double userLong_deg,
			HashMap<Integer, HashMap<Integer, Double>> IonoVDelay) {

		double userLat_rad = Math.toRadians(userLat_deg);
		double userLong_rad = Math.toRadians(userLong_deg);

		double earth_central_angle = ((Math.PI) / 2) - ElevAng_rad
				- Math.asin((Earth_Radius / (Earth_Radius + maxED_Height)) * Math.cos(ElevAng_rad));
		double IPP_lat_rad = Math.asin((Math.sin(userLat_rad) * Math.cos(earth_central_angle))
				+ (Math.cos(userLat_rad) * Math.sin(earth_central_angle) * Math.cos(AzmAng_rad)));

		boolean cond1 = (userLat_deg > 70)
				&& (Math.tan(earth_central_angle) * Math.cos(AzmAng_rad) > Math.tan(((Math.PI) / 2) - userLat_rad));
		boolean cond2 = (userLat_deg < -70) && (Math.tan(earth_central_angle) * Math.cos(AzmAng_rad + Math.PI) > Math
				.tan(((Math.PI) / 2) + userLat_rad));

		double IPP_lon_rad = 0;

		if (cond1 || cond2) {
			IPP_lon_rad = userLong_rad + Math.PI
					- Math.asin((Math.sin(earth_central_angle) * Math.sin(AzmAng_rad)) / Math.cos(IPP_lat_rad));
		} else {
			IPP_lon_rad = userLong_rad
					+ Math.asin((Math.sin(earth_central_angle) * Math.sin(AzmAng_rad)) / Math.cos(IPP_lat_rad));
		}

		double IPP_lat_deg = Math.toDegrees(IPP_lat_rad);
		double IPP_lon_deg = Math.toDegrees(IPP_lon_rad);
		double vertIonoDelay = 0;
		Square sqr = new Square(IPP_lat_deg, IPP_lon_deg, IonoVDelay);
		Triangle tri = new Triangle(IPP_lat_deg, IPP_lon_deg, IonoVDelay);
		if (IPP_lat_deg >= -60 && IPP_lat_deg <= 60) {
			sqr.checkViablilty(5, 5);
			Flag flag = sqr.getFlag();
			if (flag == Flag.TERMINATE) {
				return 0;
			} else if (flag == Flag.VIABLE) {
				vertIonoDelay = sqr.interpolate();
				double slantIonoDelay = 0;
				return slantIonoDelay;
			}
			tri.checkViablilty(5, 5);
			flag = tri.getFlag();
			if (flag == Flag.TERMINATE) {
				return 0;
			} else if (flag == Flag.VIABLE) {
				vertIonoDelay = tri.interpolate();
				double slantIonoDelay = 0;
				return slantIonoDelay;
			}

		}

		return 0;

	}

//	private double getSlantDelay(double vertIonoDelay)
//	{
//		// Obliquity Factor
//		double Fpp = Math.sqrt(1-Math.pow((Earth_Radius*Math.cos(El)), b)) 
//	}

}

class Square {

	private double y;
	private double x;

	private HashMap<Integer, HashMap<Integer, Double>> map;
	private Flag flag;
	private double val11;
	private double val21;
	private double val12;
	private double val22;
	private int x1;
	private int x2;
	private int y1;
	private int y2;

	Square(double lat, double lon, HashMap<Integer, HashMap<Integer, Double>> map) {
		this.y = lat;
		this.x = lon;

		this.map = map;
		flag = Flag.UNVIABLE;
	}

	public void checkViablilty(int lat_cell, int lon_cell) {

		int[] Y1;
		int[] X1;
		if (lat_cell == 10 && y <= 75) {
			double mod = y % 10;
			if (mod < 5) {
				Y1 = new int[2];
				Y1[0] = (int) Math.floor(y / lat_cell);
				Y1[1] = Y1[0] - 5;
			} else if (mod == 5) {
				Y1 = new int[3];
				Y1[0] = (int) y;
				Y1[1] = (int) Math.floor(y / lat_cell);
				Y1[2] = Y1[0] - 5;
			} else {
				Y1 = new int[2];
				Y1[0] = (int) Math.floor(y / lat_cell);
				Y1[1] = Y1[0] + 5;
			}
		}
		if (lon_cell == 10 && y <= 60) {
			double mod = x % 10;
			if (mod == 0) {
				X1 = new int[3];
				X1[0] = (int) x;
				X1[1] = X1[0] - 5;
				X1[2] = X1[1] - 5;
			} else if (mod < 5) {
				X1 = new int[2];
				X1[0] = (int) Math.floor(x / lon_cell) * lon_cell;
				X1[1] = X1[0] - 5;
			} else if (mod == 5) {
				X1 = new int[3];
				X1[0] = (int) x;
				X1[1] = (int) Math.floor(x / lon_cell);
				X1[2] = X1[0] - 5;
			} else {
				X1 = new int[2];
				X1[0] = (int) Math.floor(x / lon_cell);
				X1[1] = X1[0] + 5;
			}
		}

		x1 = (int) Math.floor(x / lon_cell);
		x2 = x1 + lon_cell;
		y1 = (int) Math.floor(y / lat_cell);
		y2 = y1 + lat_cell;

	}

	private void process(int x1, int x2, int y1, int y2) {
		if (map.containsKey(y1) && map.containsKey(y2)) {
			if (map.get(y1).containsKey(x1) && map.get(y1).containsKey(x2) && map.get(y2).containsKey(x1)
					&& map.get(y2).containsKey(x2)) {
				val11 = map.get(y1).get(x1);
				val21 = map.get(y1).get(x2);
				val12 = map.get(y2).get(x1);
				val22 = map.get(y2).get(x2);
				if (val11 == 100 || val12 == 100 || val21 == 100 || val22 == 100) {
					flag = Flag.TERMINATE;

				} else if (val11 == 101 || val12 == 101 || val21 == 101 || val22 == 101) {
					flag = Flag.UNVIABLE;

				} else {
					flag = Flag.VIABLE;

				}

			}

		}
	}

	public double interpolate() {
		if (flag != Flag.VIABLE) {
			System.out.println("ERROR: invlaid interpolation of IGP delay took place");

		}

		double xpp = x - x1 / (x2 - x1);
		double ypp = y - y1 / (y2 - y1);
		double W11 = (1 - xpp) * (1 - ypp);
		double W12 = xpp * (1 - ypp);
		double W21 = (1 - xpp) * ypp;
		double W22 = xpp * ypp;
		// interpolated vertical IPP delay
		double IVD = (W11 * val11) + (W12 * val12) + (W21 * val21) + (W22 * val22);

		return IVD;

	}

	public Flag getFlag() {
		return flag;
	}
}

class Triangle {

	private double y;
	private double x;

	private HashMap<Integer, HashMap<Integer, Double>> map;
	private Flag flag;
	private double val1;
	private double val2;
	private double val3;
	private int[][] triPts;

	Triangle(double lat, double lon, HashMap<Integer, HashMap<Integer, Double>> map) {
		this.y = lat;
		this.x = lon;

		this.map = map;
		flag = Flag.UNVIABLE;
	}

	public void checkViablilty(int lat_cell, int lon_cell) {
		int x1 = (int) Math.floor(x / lon_cell);
		int x2 = x1 + 5;
		int y1 = (int) Math.floor(y / lat_cell);
		int y2 = y1 + 5;

		double[] P = new double[] { x, y };
		int[][] pts = new int[4][2];
		pts[0] = new int[] { x1, y1 };
		pts[1] = new int[] { x1, y2 };
		pts[2] = new int[] { x2, y2 };
		pts[3] = new int[] { x2, y1 };

		for (int i = 0; i < 4; i++) {
			triPts = new int[3][2];
			for (int j = i; j < i + 3; j++) {
				int k = j % 4;
				triPts[k] = pts[k];
			}
			if (isInside(triPts, P)) {

				int[] P1 = triPts[0];
				int[] P2 = triPts[1];
				int[] P3 = triPts[2];
				if (map.containsKey(P1[1]) && map.containsKey(P3[1])) {
					if (map.get(P1[1]).containsKey(P1[0]) && map.get(P2[1]).containsKey(P2[0])
							&& map.get(P3[1]).containsKey(P3[0])) {

						val1 = map.get(P1[1]).get(P1[0]);
						val2 = map.get(P2[1]).get(P2[0]);
						val3 = map.get(P3[1]).get(P3[0]);
						if (val1 == 101 || val2 == 101 || val3 == 101) {
							flag = Flag.UNVIABLE;
							continue;
						} else {
							flag = Flag.VIABLE;
							break;
						}
					}

				}

			}
		}

	}

	public double interpolate() {
		if (flag != Flag.VIABLE) {
			System.out.println("ERROR: invlaid interpolation of IGP delay took place");

		}
		int[] P1 = triPts[0];
		int[] P2 = triPts[1];
		int[] P3 = triPts[2];

		double dX = Math.abs(x - P2[0]);
		double dY = Math.abs(y - P2[1]);
		int[] V21 = vecSub(P1, P2);
		int[] V23 = vecSub(P3, P2);

		double W1;
		double W2;
		double W3;
		if (V21[0] == 0) {
			double ypp = dY / Math.abs(V21[1]);
			double xpp = dX / Math.abs(V23[0]);
			W1 = ypp;
			W2 = 1 - xpp - ypp;
			W3 = xpp;
		} else {
			double ypp = dY / Math.abs(V21[0]);
			double xpp = dX / Math.abs(V23[1]);
			W1 = xpp;
			W2 = 1 - xpp - ypp;
			W3 = ypp;
		}

		// interpolated vertical IPP delay
		double IVD = (W1 * val1) + (W2 * val2) + (W3 * val3);

		return IVD;

	}

	public Flag getFlag() {
		return flag;
	}

	private boolean isInside(int[][] triPts, double[] P) {

		int[] P1 = triPts[0];
		int[] P2 = triPts[1];
		int[] P3 = triPts[2];

		double[] v0 = new double[] { P3[0] - P1[0], P3[1] - P1[1] };
		double[] v1 = new double[] { P2[0] - P1[0], P2[1] - P1[1] };
		double[] v2 = new double[] { P[0] - P1[0], P[1] - P1[1] };

		double dot00 = dotProd(v0, v0);
		double dot01 = dotProd(v0, v1);
		double dot02 = dotProd(v0, v2);
		double dot11 = dotProd(v1, v1);
		double dot12 = dotProd(v1, v2);

		// Compute barycentric coordinates
		double invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
		double u = (dot11 * dot02 - dot01 * dot12) * invDenom;
		double v = (dot00 * dot12 - dot01 * dot02) * invDenom;

		// Check if point is in triangle
		return (u >= 0) && (v >= 0) && (u + v < 1);

	}

	private double dotProd(double P1[], double P2[]) {
		return (P1[0] * P2[0]) + (P1[1] * P2[1]);
	}

	private int[] vecSub(int P1[], int P2[]) {
		return new int[] { P1[0] - P2[0], P1[1] - P2[1] };
	}
}
