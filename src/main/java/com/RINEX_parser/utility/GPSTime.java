package com.RINEX_parser.utility;

import java.util.Calendar;
import java.util.TimeZone;

public class GPSTime {

	public static long getGPSTime(int year, int month, int day, int hour, int minute, int sec) {

		long NumberSecondsWeek = 604800;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(year, month, day, hour, minute, sec);
		long unixTime = cal.getTimeInMillis();
		cal.set(1980, 0, 6, 0, 0, 0);
		long GPSEpoch = cal.getTimeInMillis();
		long GPSTime = ((unixTime - GPSEpoch) / 1000) % NumberSecondsWeek;
		long weekNo = ((unixTime - GPSEpoch) / 1000) / NumberSecondsWeek;
		// System.out.println(GPSTime + " " + weekNo);
		return GPSTime;
	}

}
