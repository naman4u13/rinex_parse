package com.RINEX_parser.helper.SBAS;

import java.util.HashMap;

import com.RINEX_parser.utility.LatLonUtil;

public class ComputeIonoCorr {

	private Flag flag = Flag.UNVIABLE;

	private double ElevAng_rad;
	private final static double Earth_Radius = 6378136.3;// in meter
	private final static double maxED_Height = 350000;// Height of Max Electron Density in meter
	private final static double SpeedofLight = 299792458;

	public double computeIonoCorr(double ElevAng_rad, double AzmAng_rad, double userLat_deg, double userLong_deg,
			HashMap<Integer, HashMap<Integer, Double>> IonoVDelay) {

		flag = Flag.UNVIABLE;
		this.ElevAng_rad = ElevAng_rad;
		double userLat_rad = userLat_deg * Math.PI / 180;
		double userLong_rad = userLong_deg * Math.PI / 180;

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

		double IPP_lat_deg = IPP_lat_rad * 180 / Math.PI;
		double IPP_lon_deg = IPP_lon_rad * 180 / Math.PI;
		if (IPP_lon_deg == 180) {
			IPP_lon_deg *= -1;
		}

		Square sqr = new Square(IPP_lat_deg, IPP_lon_deg, IonoVDelay);
		Triangle tri = new Triangle(IPP_lat_deg, IPP_lon_deg, IonoVDelay);
		if (IPP_lat_deg >= -60 && IPP_lat_deg <= 60) {
			sqr.checkViablilty(5, 5);
			flag = sqr.getFlag();
			if (flag != Flag.UNVIABLE) {
				return getIonoDelay(sqr);
			}
			tri.checkViablilty(5, 5);
			flag = tri.getFlag();
			if (flag != Flag.UNVIABLE) {
				return getIonoDelay(tri);
			}
			sqr.checkViablilty(10, 10);
			flag = sqr.getFlag();
			if (flag != Flag.UNVIABLE) {
				return getIonoDelay(sqr);
			}
			tri.checkViablilty(10, 10);
			flag = tri.getFlag();
			if (flag != Flag.UNVIABLE) {
				return getIonoDelay(tri);
			}
			// No Ionospheric Correction Available
			return 0;

		} else if (IPP_lat_deg > 60 && IPP_lat_deg <= 75 || IPP_lat_deg < -60 && IPP_lat_deg >= -75) {
			sqr.checkViablilty(5, 10);
			flag = sqr.getFlag();
			if (flag != Flag.UNVIABLE) {
				return getIonoDelay(sqr);
			}
			tri.checkViablilty(5, 10);
			flag = tri.getFlag();
			if (flag != Flag.UNVIABLE) {
				return getIonoDelay(tri);
			}
			sqr.checkViablilty(10, 10);
			flag = sqr.getFlag();
			if (flag != Flag.UNVIABLE) {
				return getIonoDelay(sqr);
			}
			tri.checkViablilty(10, 10);
			flag = tri.getFlag();
			if (flag != Flag.UNVIABLE) {
				return getIonoDelay(tri);
			}
			// No Ionospheric Correction Available
			return 0;

		} else {
			System.out.println("ERROR ABOVE 75 lat");
		}

		return 0;

	}

	private double getIonoDelay(Polygon poly) {
		double IonoDelay = 0;
		if (flag == Flag.VIABLE) {
			double vertIonoDelay = poly.interpolate();
			// Obliquity Factor
			double Fpp = 1 / Math
					.sqrt(1 - Math.pow((Earth_Radius * Math.cos(ElevAng_rad)) / (Earth_Radius + maxED_Height), 2));
			// Slant Delay
			IonoDelay = Fpp * vertIonoDelay;

		}
		return IonoDelay;

	}

	public Flag getIonoFlag() {
		return flag;
	}

	private double IPP7585(double lat, double lon) {
		int y1 = 75;
		int y2 = 85;
		int[] X1_75;
		if (lon % 10 == 0) {
			X1_75 = new int[2];
			X1_75[0] = (int) lon;
			X1_75[1] = LatLonUtil.lonAdd(X1_75[0], -10);
		} else {
			X1_75 = new int[] { ((int) Math.floor(lon / 10)) * 10 };
		}
		int[] X1_85;
		int res = 0;
		if (lat < 0) {
			res = -10;
		}

		if ((lon + res) % 30 == 0) {
			X1_85 = new int[2];
			X1_85[0] = (int) lon;
			X1_85[1] = LatLonUtil.lonAdd(X1_85[0], -30);
		} else {

			X1_85 = new int[] { LatLonUtil.lonAdd(((int) Math.floor((lon + res) / 30) * 30), -res) };
		}

		return 0;
	}

}
