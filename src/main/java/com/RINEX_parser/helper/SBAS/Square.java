package com.RINEX_parser.helper.SBAS;

import java.util.HashMap;

import com.RINEX_parser.utility.LatLonUtil;

public class Square extends Polygon {

	private double val11;
	private double val21;
	private double val12;
	private double val22;
	private int x1;
	private int x2;
	private int y1;
	private int y2;

	Square(double lat, double lon, HashMap<Integer, HashMap<Integer, Double>> map) {
		super(lat, lon, map);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void beginCheck(int x1, int y1, int x2, int y2) {
		HashMap<Integer, HashMap<Integer, Double>> map = getMap();
		if (map.containsKey(y1) && map.containsKey(y2)) {
			if (map.get(y1).containsKey(x1) && map.get(y1).containsKey(x2) && map.get(y2).containsKey(x1)
					&& map.get(y2).containsKey(x2)) {
				val11 = map.get(y1).get(x1);
				val21 = map.get(y1).get(x2);
				val12 = map.get(y2).get(x1);
				val22 = map.get(y2).get(x2);
				if (val11 == 100 || val12 == 100 || val21 == 100 || val22 == 100) {
					setFlag(Flag.TERMINATE);
					return;

				} else if (val11 == 101 || val12 == 101 || val21 == 101 || val22 == 101) {
					setFlag(Flag.UNVIABLE);

				} else {

					setFlag(Flag.VIABLE);
					this.x1 = x1;
					this.x2 = x2;
					this.y1 = y1;
					this.y2 = y2;
					return;

				}

			}

		}
	}

	@Override
	public double interpolate() {
		// TODO Auto-generated method stub
		if (getFlag() != Flag.VIABLE) {
			System.out.println("ERROR: invlaid interpolation of IGP delay took place");

		}

		double xpp = getX() - x1 / (x2 - x1);
		double ypp = LatLonUtil.lonDiff(y1, getY()) / LatLonUtil.lonDiff(y1, y2);
		double W11 = (1 - xpp) * (1 - ypp);
		double W12 = xpp * (1 - ypp);
		double W21 = (1 - xpp) * ypp;
		double W22 = xpp * ypp;
		// interpolated vertical IPP delay
		double IVD = (W11 * val11) + (W12 * val12) + (W21 * val21) + (W22 * val22);

		return IVD;
	}

}
