package com.RINEX_parser.helper.SBAS;

import java.util.HashMap;

import com.RINEX_parser.utility.LatLonUtil;

public abstract class Polygon {

	private double x;
	private double y;
	private HashMap<Integer, HashMap<Integer, Double>> map;
	private Flag flag;

	Polygon(double lat, double lon, HashMap<Integer, HashMap<Integer, Double>> map) {
		this.y = lat;
		this.x = lon;

		this.map = map;
		flag = Flag.UNVIABLE;
	}

	public Flag getFlag() {
		return flag;
	}

	public void setFlag(Flag flag) {
		this.flag = flag;
	}

	// Finding out all possible cell windows
	public int[][] winSelect(int lat_cell, int lon_cell) {
		int[] Y1;
		int[] X1;

		double mod = Math.abs(y % lat_cell);
		int n = lat_cell / 5;
		if (mod == 0 || mod == 5) {
			Y1 = new int[n + 1];
			Y1[0] = (int) y;
			int diff = (y > 0 ? -5 : +5);
			for (int i = 1; i <= n; i++) {
				Y1[i] = Y1[i - 1] + diff;
			}
		} else if (mod < 5) {
			Y1 = new int[n];
			Y1[0] = ((int) (y / lat_cell)) * lat_cell;
			int diff = (y > 0 ? -5 : +5);
			for (int i = 1; i < n; i++) {
				Y1[i] = Y1[i - 1] + diff;
			}
		} else {
			Y1 = new int[2];
			Y1[0] = ((int) (y / lat_cell)) * lat_cell;
			int diff = (y > 0 ? 5 : -5);
			Y1[1] = Y1[0] + diff;
		}

		if (Math.abs(y) <= 60) {
			mod = Math.abs(x % lon_cell);
			n = lon_cell / 5;
			if (mod == 0 || mod == 5) {
				X1 = new int[n + 1];
				X1[0] = (int) x;
				int diff = (x > 0 ? -5 : +5);
				for (int i = 1; i <= n; i++) {
					X1[i] = LatLonUtil.lonAdd(X1[i - 1], diff);

				}
			} else if (mod < 5) {
				X1 = new int[n];
				X1[0] = ((int) (x / lon_cell)) * lon_cell;
				int diff = (x > 0 ? -5 : +5);
				for (int i = 1; i < n; i++) {
					X1[i] = LatLonUtil.lonAdd(X1[i - 1], diff);

				}
			} else {
				X1 = new int[2];
				X1[0] = ((int) (x / lon_cell)) * lon_cell;
				int diff = (x > 0 ? 5 : -5);
				X1[1] = LatLonUtil.lonAdd(X1[0], diff);

			}
		} else { // In the case when lat is in (60-75) range and lon spacing is 10 deg

			mod = Math.abs(x % lon_cell);
			if (mod == 0) {
				X1 = new int[2];
				X1[0] = (int) x;
				int diff = (x > 0 ? -10 : +10);
				X1[1] = LatLonUtil.lonAdd(X1[0], diff);
			} else {
				X1 = new int[1];
				X1[0] = ((int) (x / lon_cell)) * lon_cell;
			}

		}

		int[][] win = new int[2][];
		win[0] = X1;
		win[1] = Y1;
		return win;
	}

	public void checkViablilty(int lat_cell, int lon_cell) {

		int[][] win = winSelect(lat_cell, lon_cell);
		int[] X1 = win[0];
		int[] Y1 = win[1];

		for (int i = 0; i < X1.length; i++) {
			int x1 = X1[i];
			int diffX = x < 0 ? -lon_cell : lon_cell;
			int x2 = LatLonUtil.lonAdd(x1, diffX);
			for (int j = 0; j < Y1.length; j++) {

				int y1 = Y1[j];
				int diffY = y < 0 ? -lat_cell : lat_cell;
				int y2 = y1 + diffY;

				beginCheck(x1, y1, x2, y2);
			}
			if (flag != Flag.UNVIABLE) {
				break;
			}
		}

	}

	public abstract double interpolate();

	public abstract void beginCheck(int x1, int y1, int x2, int y2);

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public HashMap<Integer, HashMap<Integer, Double>> getMap() {
		return map;
	}

}
