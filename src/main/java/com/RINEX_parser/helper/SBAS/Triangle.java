package com.RINEX_parser.helper.SBAS;

import java.util.HashMap;

import com.RINEX_parser.utility.LatLonUtil;

public class Triangle extends Polygon {

	private double val1;
	private double val2;
	private double val3;
	private int[][] triPts;

	Triangle(double lat, double lon, HashMap<Integer, HashMap<Integer, Double>> map) {
		super(lat, lon, map);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void beginCheck(int x1, int y1, int x2, int y2) {
		// TODO Auto-generated method stub
		HashMap<Integer, HashMap<Integer, Double>> map = getMap();
		double[] P = new double[] { getX(), getY() };
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
							setFlag(Flag.UNVIABLE);
							continue;
						} else {
							setFlag(Flag.VIABLE);
							return;
						}
					}

				}

			}
		}

	}

	@Override
	public double interpolate() {
		if (getFlag() != Flag.VIABLE) {
			System.out.println("ERROR: invlaid interpolation of IGP delay took place");

		}
		int[] P1 = triPts[0];
		int[] P2 = triPts[1];
		int[] P3 = triPts[2];

		double dX = Math.abs(getX() - P2[0]);
		double dY = Math.abs(getY() - P2[1]);
		int[] V21 = vecSub(P2, P1);
		int[] V23 = vecSub(P2, P3);

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
			double ypp = dY / Math.abs(V23[1]);
			double xpp = dX / Math.abs(V21[0]);
			W1 = xpp;
			W2 = 1 - xpp - ypp;
			W3 = ypp;
		}

		// interpolated vertical IPP delay
		double IVD = (W1 * val1) + (W2 * val2) + (W3 * val3);

		return IVD;

	}

	private boolean isInside(int[][] triPts, double[] P) {

		int[] P1 = triPts[0];
		int[] P2 = triPts[1];
		int[] P3 = triPts[2];

		double[] v0 = new double[] { LatLonUtil.lonDiff(P1[0], P3[0]), P3[1] - P1[1] };
		double[] v1 = new double[] { LatLonUtil.lonDiff(P1[0], P2[0]), P2[1] - P1[1] };
		double[] v2 = new double[] { LatLonUtil.lonDiff(P1[0], P[0]), P[1] - P1[1] };

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
		return new int[] { (int) LatLonUtil.lonDiff(P1[0], P2[0]), P2[1] - P1[1] };
	}
}
