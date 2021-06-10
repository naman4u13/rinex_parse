package com.RINEX_parser.helper;

public class ComputeIPP {

	public static double[] computeIPP(double ElevAng_rad, double AzmAng_rad, double userLat_deg, double userLong_deg,
			double Re, double h) {
		double userLat_rad = userLat_deg * Math.PI / 180;
		double userLong_rad = userLong_deg * Math.PI / 180;
//		Re = 6378136.3;
//		h = 350000;
		double earth_central_angle = ((Math.PI) / 2) - ElevAng_rad - Math.asin((Re / (Re + h)) * Math.cos(ElevAng_rad));
		double IPP_lat_rad = Math.asin((Math.sin(userLat_rad) * Math.cos(earth_central_angle))
				+ (Math.cos(userLat_rad) * Math.sin(earth_central_angle) * Math.cos(AzmAng_rad)));

		boolean cond1 = (userLat_deg > 70)
				&& ((Math.tan(earth_central_angle) * Math.cos(AzmAng_rad)) > (Math.tan(((Math.PI) / 2) - userLat_rad)));
		boolean cond2 = (userLat_deg < -70) && ((Math.tan(earth_central_angle) * Math.cos(AzmAng_rad + Math.PI)) > (Math
				.tan(((Math.PI) / 2) + userLat_rad)));

		double IPP_lon_rad = 0;

		if (cond1 || cond2) {
			IPP_lon_rad = userLong_rad + Math.PI
					- Math.asin((Math.sin(earth_central_angle) * Math.sin(AzmAng_rad)) / Math.cos(IPP_lat_rad));
		} else {
			IPP_lon_rad = userLong_rad
					+ Math.asin((Math.sin(earth_central_angle) * Math.sin(AzmAng_rad)) / Math.cos(IPP_lat_rad));
		}
		IPP_lon_rad %= (2 * Math.PI);
		if (IPP_lon_rad > Math.PI) {
			IPP_lon_rad -= (2 * Math.PI);
		} else if (IPP_lon_rad < -Math.PI) {
			IPP_lon_rad += (2 * Math.PI);
		}

		double IPP_lat_deg = (IPP_lat_rad * 180 / Math.PI);
		double IPP_lon_deg = (IPP_lon_rad * 180 / Math.PI);

		return new double[] { IPP_lat_deg, IPP_lon_deg };

	}

}
