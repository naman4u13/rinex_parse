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
				&& ((Math.tan(earth_central_angle) * Math.cos(AzmAng_rad)) > (Math.tan((Math.PI / 2) - userLat_rad)));
		boolean cond2 = (userLat_deg < -70) && ((Math.tan(earth_central_angle) * Math.cos(AzmAng_rad + Math.PI)) > (Math
				.tan((Math.PI / 2) + userLat_rad)));

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

		double temp_earth_central_angle = (0.0137 / ((ElevAng_rad / Math.PI) + 0.11)) - 0.022;
		// double temp_earth_central_angle = earth_central_angle / Math.PI;
		double temp_IPP_lat = (userLat_rad / Math.PI) + (temp_earth_central_angle * Math.cos(AzmAng_rad));
		if (temp_IPP_lat > 0.416) {
			temp_IPP_lat = 0.416;
		} else if (temp_IPP_lat < -0.416) {
			temp_IPP_lat = -0.416;
		}
		double temp_IPP_long = (userLong_rad / Math.PI)
				+ ((temp_earth_central_angle * Math.sin(AzmAng_rad)) / Math.cos(temp_IPP_lat * Math.PI));

		double lat = temp_IPP_lat * 180;
		double lon = temp_IPP_long * 180;

		if (Math.abs(IPP_lat_deg) > 70) {
			System.err.println("user Lat went above |70|");
		}
		if (Math.abs(lat - IPP_lat_deg) > 3.5 || Math.abs(lon - IPP_lon_deg) > 3.5) {
			System.err.println("lat Diff is big");
		}

		return new double[] { IPP_lat_deg, IPP_lon_deg };

	}

}
