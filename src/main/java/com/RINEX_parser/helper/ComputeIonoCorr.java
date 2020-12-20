package com.RINEX_parser.helper;

import com.RINEX_parser.models.IonoCoeff;

public class ComputeIonoCorr {
	final static double Earth_Radius = 6378136.3;// in meter
	final static double max_electron_density = 350000;// in meter
	final static double SpeedofLight = 299792458;

	public static double[] computeIonoCorr(double ElevAng_rad, double AzmAng_rad, double userLat_deg,
			double userLong_deg, long tSV, IonoCoeff ionoCoeff) {

		/*
		 * double Central_Angle = (0.0137/(ElevAng+0.11))-0.022; double IPP_Lat =
		 */
		tSV = tSV % 86400;
		// double ElevAng_rad = Math.toRadians(ElevAng_deg);
		// double AzmAng_rad = Math.toRadians(AzmAng_deg);
		double userLat_rad = Math.toRadians(userLat_deg);
		double userLong_rad = Math.toRadians(userLong_deg);
		double earth_central_angle = ((Math.PI) / 2) - ElevAng_rad
				- Math.asin(((Earth_Radius) / (Earth_Radius + max_electron_density)) * Math.cos(ElevAng_rad));
		double IPP_lat = Math.asin((Math.sin(userLat_rad) * Math.cos(earth_central_angle))
				+ (Math.cos(userLat_rad) * Math.sin(earth_central_angle) * Math.cos(AzmAng_rad)));
		double IPP_long = userLong_rad
				+ Math.asin((Math.sin(earth_central_angle) * Math.sin(AzmAng_rad)) / Math.cos(IPP_lat));

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

		double geomagnetic_lat = (IPP_lat / Math.PI) + (0.064 * Math.cos(((IPP_long / Math.PI) - 1.617) * Math.PI));
		double temp_geomagnetic_lat = temp_IPP_lat + (0.064 * Math.cos((temp_IPP_long - 1.617) * Math.PI));

		double local_time = 4.32 * (1E4) * (IPP_long / Math.PI) + tSV;
		if (local_time >= 86400) {
			local_time = local_time - 86400;
		} else if (local_time < 0) {
			local_time = local_time + 86400;
		}
		double temp_local_time = 4.32 * (1E4) * (temp_IPP_long) + tSV;
		if (temp_local_time >= 86400) {
			temp_local_time = temp_local_time - 86400;
		} else if (local_time < 0) {
			temp_local_time = temp_local_time + 86400;
		}

		double obliquity_factor = 1 + (16 * (Math.pow(0.53 - (ElevAng_rad / Math.PI), 3)));

		double PER = ionoCoeff.getBeta0() + (ionoCoeff.getBeta1() * geomagnetic_lat)
				+ (ionoCoeff.getBeta2() * Math.pow(geomagnetic_lat, 2))
				+ (ionoCoeff.getBeta3() * Math.pow(geomagnetic_lat, 3));
		if (PER < 72000) {
			PER = 72000;
		}
		double temp_PER = ionoCoeff.getBeta0() + (ionoCoeff.getBeta1() * temp_geomagnetic_lat)
				+ (ionoCoeff.getBeta2() * Math.pow(temp_geomagnetic_lat, 2))
				+ (ionoCoeff.getBeta3() * Math.pow(temp_geomagnetic_lat, 3));
		if (temp_PER < 72000) {
			temp_PER = 72000;
		}

		double phase = (2 * Math.PI * (local_time - 50400)) / PER;
		double temp_phase = (2 * Math.PI * (temp_local_time - 50400)) / temp_PER;

		double AMP = ionoCoeff.getAlpha0() + (ionoCoeff.getAlpha1() * geomagnetic_lat)
				+ (ionoCoeff.getAlpha2() * Math.pow(geomagnetic_lat, 2))
				+ (ionoCoeff.getAlpha3() * Math.pow(geomagnetic_lat, 3));
		if (AMP < 0) {
			AMP = 0;
		}
		double temp_AMP = ionoCoeff.getAlpha0() + (ionoCoeff.getAlpha1() * temp_geomagnetic_lat)
				+ (ionoCoeff.getAlpha2() * Math.pow(temp_geomagnetic_lat, 2))
				+ (ionoCoeff.getAlpha3() * Math.pow(temp_geomagnetic_lat, 3));
		if (temp_AMP < 0) {
			AMP = 0;
		}

		double iono_time;
		if (Math.abs(phase) < 1.57) {
			iono_time = obliquity_factor * (5E-9 + (AMP * (1 - (Math.pow(phase, 2) / 2) + (Math.pow(phase, 4) / 24))));
		} else {
			iono_time = obliquity_factor * 5E-9;
		}
		double iono_corr = iono_time * SpeedofLight;
		double timediff = local_time - tSV;
		double temp_iono_time;
		if (Math.abs(temp_phase) < 1.57) {
			temp_iono_time = obliquity_factor
					* (5E-9 + (temp_AMP * (1 - (Math.pow(temp_phase, 2) / 2) + (Math.pow(temp_phase, 4) / 24))));
		} else {
			temp_iono_time = obliquity_factor * 5E-9;
		}
		double temp_iono_corr = temp_iono_time * SpeedofLight;
		return new double[] { iono_corr, local_time - tSV };

		/*
		 * temp_earth_central_angle = temp_earth_central_angle * 180; temp_IPP_lat =
		 * temp_IPP_lat * 180; temp_IPP_long = temp_IPP_long * 180; earth_central_angle
		 * = earth_central_angle * (180 / Math.PI); IPP_lat = IPP_lat * (180 / Math.PI);
		 * IPP_long = IPP_long * (180 / Math.PI); geomagnetic_lat = geomagnetic_lat *
		 * 180; temp_geomagnetic_lat = temp_geomagnetic_lat * 180;
		 * 
		 * System.out.println("  temp_earth_central_angle " + temp_earth_central_angle);
		 * System.out.println("  earth_central_angle " + earth_central_angle);
		 * System.out.println("  temp_IPP_lat " + temp_IPP_lat);
		 * System.out.println("  IPP_lat " + IPP_lat);
		 * System.out.println("  temp_IPP_long " + temp_IPP_long);
		 * System.out.println("  IPP_long " + IPP_long);
		 * System.out.println("  temp_geomagnetic_lat  " + temp_geomagnetic_lat);
		 * System.out.println("  geomagnetic_lat  " + geomagnetic_lat);
		 * System.out.println("  temp_local_time  " + temp_local_time);
		 * System.out.println("  local_time  " + local_time); System.out.println(1E4);
		 */
//6.691617002417778
	}

	public static double computeIonoCorr2(double ElevAng_rad, double AzmAng_rad, double userLat_deg,
			double userLong_deg, long tSV, IonoCoeff ionoCoeff) {

		// Computing each angle in semi-circles
		double userLat = userLat_deg / 180;
		double userLong = userLong_deg / 180;
		double ElevAng = ElevAng_rad / Math.PI;
		double AzmAng = AzmAng_rad / Math.PI;

		double earth_central_angle = (0.0137 / (ElevAng + 0.11)) - 0.022;

		return 0.0;
	}

}
