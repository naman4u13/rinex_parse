package com.RINEX_parser.utility;

import java.util.Calendar;
import java.util.TimeZone;

public class Time {
	private static final long NumberSecondsWeek = 604800;

	// Convert GPS timestamp to GPS TOW and Week No.
	// Note this func does not handle conversion of UTC timestamp to GPS time
	// Java does not support handling leap seconds, so Calendar or Date classes will
	// not account for leap seconds
	public static long[] getGPSTime(int year, int month, int day, int hour, int minute, int sec) {

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(year, month, day, hour, minute, sec);
		// Nomenclature is unixTime because getTimeInMillis does not account for leap
		// seconds
		long unixTime = cal.getTimeInMillis();
		cal.set(1980, 0, 6, 0, 0, 0);
		long GPSEpoch = cal.getTimeInMillis();
		long GPSTime = ((unixTime - GPSEpoch) / 1000) % NumberSecondsWeek;
		long weekNo = ((unixTime - GPSEpoch) / 1000) / NumberSecondsWeek;

		return new long[] { GPSTime, weekNo };
	}

	public static long[] getGPSTime(Calendar time) {

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		long unixTime = time.getTimeInMillis();
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
		// GPS time in local TIMEZONE

		long localGPSTime = (long) (4.32 * 1000 * (1E4) * (longitude / 180)
				+ (GPSTime * 1000 + (weekNo * NumberSecondsWeek * 1000)));
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(1980, 0, 6, 0, 0, 0);
		long GPSEpoch = cal.getTimeInMillis();
		long unixTime = (localGPSTime) + GPSEpoch;
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
