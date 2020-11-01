package com.RINEX_parser.utility;

public class LatLonDiff {
	public static double distance(double[] LatLon1, double[] LatLon2) {

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

}
