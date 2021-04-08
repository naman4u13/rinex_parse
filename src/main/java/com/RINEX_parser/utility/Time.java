package com.RINEX_parser.utility;

import java.util.Calendar;
import java.util.TimeZone;

public class Time {
	static long NumberSecondsWeek = 604800;

	public static long[] getGPSTime(int year, int month, int day, int hour, int minute, int sec) {

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(year, month, day, hour, minute, sec);
		long unixTime = cal.getTimeInMillis();
		cal.set(1980, 0, 6, 0, 0, 0);
		long GPSEpoch = cal.getTimeInMillis();
		long GPSTime = ((unixTime - GPSEpoch) / 1000) % NumberSecondsWeek;
		long weekNo = ((unixTime - GPSEpoch) / 1000) / NumberSecondsWeek;
		// System.out.println(Time + " " + weekNo);
		return new long[] { GPSTime, weekNo };
	}

	public static long getUnixTimefromEpoch(int year, int month, int day, int hour, int minute, int sec) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(year, month, day, hour, minute, sec);
		long unixTime = cal.getTimeInMillis();
		return unixTime / 1000;

	}

	public static Calendar getDate(long GPSTime, long weekNo, double longitude) {
		// There is no such thing as local GPS Time, the variable is a way of
		// representing
		// time in local TIMEZONE

		long localGPSTime = (long) (4.32 * (1E4) * (longitude / 180) + (GPSTime + (weekNo * NumberSecondsWeek)));
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(1980, 0, 6, 0, 0, 0);
		long GPSEpoch = cal.getTimeInMillis();
		long unixTime = (localGPSTime * 1000) + GPSEpoch;
		cal.setTimeInMillis(unixTime);
		return cal;

	}

	public static Calendar convertToUTC(Calendar localGPStime, double longitude) {

		long UTCtime = (localGPStime.getTimeInMillis()) - (long) (4.32 * (1E4) * (longitude / 180) * 1000);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(UTCtime);
		return cal;

	}
}
