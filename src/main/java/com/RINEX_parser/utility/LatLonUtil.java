package com.RINEX_parser.utility;

public class LatLonUtil {

	// All are WGS-84 params
	// Semi-major axis or Equatorial radius
	public static final double a = 6378137;
	// flattening
	public static final double f = 1 / 298.257223563;
	// Semi-minor axis or Polar radius
	public static final double b = 6356752.314245;
	public static final double e2 = Math.sqrt((Math.pow(a, 2) - Math.pow(b, 2)) / Math.pow(b, 2));

	public static double getHaversineDistance(double[] LatLon1, double[] LatLon2) {

		double lat1 = LatLon1[0];
		double lon1 = LatLon1[1];
		double lat2 = LatLon2[0];
		double lon2 = LatLon2[1];
		// The math module contains a function named toRadians which converts from
		// degrees to radians.
		lon1 = Math.toRadians(lon1);
		lon2 = Math.toRadians(lon2);
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);

		// Haversine formula
		double dlon = lon2 - lon1;
		double dlat = lat2 - lat1;
		double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);

		double c = 2 * Math.asin(Math.sqrt(a));

		// Used Mean Earth Radius
		double r = 6371000;

		// calculate the result
		return (c * r);
	}

	public static double getVincentyDistance(double[] LatLon1, double[] LatLon2) {
		double lat1 = LatLon1[0];
		double lon1 = LatLon1[1];
		double lat2 = LatLon2[0];
		double lon2 = LatLon2[1];

		double L = Math.toRadians(lon2 - lon1);

		double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat1)));

		double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat2)));

		double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);

		double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

		double cosSqAlpha, sinSigma, cos2SigmaM, cosSigma, sigma;

		double lambda = L, lambdaP, iterLimit = 100;

		do {

			double sinLambda = Math.sin(lambda), cosLambda = Math.cos(lambda);

			sinSigma = Math.sqrt((cosU2 * sinLambda)

					* (cosU2 * sinLambda)

					+ (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)

							* (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)

			);

			if (sinSigma == 0)
				return 0;

			cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;

			sigma = Math.atan2(sinSigma, cosSigma);

			double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;

			cosSqAlpha = 1 - sinAlpha * sinAlpha;

			cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;

			double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));

			lambdaP = lambda;

			lambda = L + (1 - C) * f * sinAlpha
					* (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));

		} while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);

		if (iterLimit == 0)
			return 0;

		double uSq = cosSqAlpha * (a * a - b * b) / (b * b);

		double A = 1 + uSq / 16384

				* (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));

		double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));

		double deltaSigma =

				B * sinSigma

						* (cos2SigmaM + B / 4

								* (cosSigma

										* (-1 + 2 * cos2SigmaM * cos2SigmaM)
										- B / 6 * cos2SigmaM

												* (-3 + 4 * sinSigma * sinSigma)

												* (-3 + 4 * cos2SigmaM * cos2SigmaM)));

		double s = b * A * (sigma - deltaSigma);
		return s;
	}

	public static int lonAdd(int lon, int diff) {
		lon = lon + diff;
		if (lon >= 180) {
			lon -= 360;
		} else if (lon < -180) {
			lon += 360;
		}
		return lon;
	}

	public static double lonAddD(double lon, int diff) {
		lon = lon + diff;
		if (lon >= 180) {
			lon -= 360;
		} else if (lon < -180) {
			lon += 360;
		}
		return lon;
	}

	public static double lonAddDD(double lon, double diff) {
		lon = lon + diff;
		if (lon >= 180) {
			lon -= 360;
		} else if (lon < -180) {
			lon += 360;
		}
		return lon;
	}

	// Only for IGP grid selection
	// accounting for continuous longitude
	public static double lonDiff(double x1, double x2) {
		// Because the IGP grid class/algo this func will be used will only work with
		// lats below 75, therefore lon
		// spacing will never be greater than 10
		double diff;
		if (Math.abs(x2 - x1) > 10) {

			if (x2 < 0) {
				diff = 360 - x1 + x2;
				return diff;
			}
			diff = -360 - x1 + x2;
			return diff;
		}
		diff = x2 - x1;
		return diff;
	}

	public static double[] ENUtoECEF(double[] enu, double[] ECEFr) {
		double[] LLH = ECEFtoLatLon.ecef2lla(ECEFr);
		double lat = LLH[0];
		double lon = LLH[1];
		double[] ECEF = new double[3];
		ECEF[0] = (-Math.sin(lon) * enu[0]) + (-Math.sin(lat) * Math.cos(lon) * enu[1])
				+ (Math.cos(lat) * Math.cos(lon) * enu[2]) + ECEFr[0];
		ECEF[1] = (Math.cos(lon) * enu[0]) + (-Math.sin(lat) * Math.sin(lon) * enu[1])
				+ (Math.cos(lat) * Math.sin(lon) * enu[2]) + ECEFr[1];
		ECEF[2] = (0 * enu[0]) + (Math.cos(lat) * enu[1]) + (Math.sin(lat) * enu[2]) + ECEFr[2];
		return ECEF;
	}

	// Geodetic Latitude to Geocentric Latitude
	public static double gd2gc(double gdLat, double gdAlt) {

		gdLat = Math.toRadians(gdLat);

		double N = a / Math.sqrt(1 - (e2 * Math.pow(Math.sin(gdLat), 2)));
		double gcLat = Math.atan((1 - (e2 * (N / (N + gdAlt)))) * Math.tan(gdLat));
		gcLat = Math.toDegrees(gcLat);
		return gcLat;

	}
}
