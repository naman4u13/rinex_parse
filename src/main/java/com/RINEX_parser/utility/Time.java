package com.RINEX_parser.utility;

import java.util.Calendar;
import java.util.TimeZone;

public class Time {
	private static final long NumberMilliSecondsWeek = 604800000;

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
		long GPSTime = ((unixTime - GPSEpoch)) % NumberMilliSecondsWeek;
		long weekNo = ((unixTime - GPSEpoch)) / NumberMilliSecondsWeek;
		GPSTime = Math.round((double) GPSTime / 1000);
		return new long[] { GPSTime, weekNo };
	}

	public static long[] getGPSTime(Calendar time) {

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		long unixTime = time.getTimeInMillis();
		cal.set(1980, 0, 6, 0, 0, 0);
		long GPSEpoch = cal.getTimeInMillis();
		long GPSTime = ((unixTime - GPSEpoch)) % NumberMilliSecondsWeek;
		long weekNo = ((unixTime - GPSEpoch)) / NumberMilliSecondsWeek;
		GPSTime = Math.round((double) GPSTime / 1000);
		return new long[] { GPSTime, weekNo };
	}

	public static Calendar getDate(long GPSTime, long weekNo, double longitude) {
		// There is no such thing as local GPS Time, the variable is a way of
		// representing
		// GPS time in local TIMEZONE

		long localGPSTime = (long) (4.32 * 1000 * (1E4) * (longitude / 180)
				+ (GPSTime * 1000 + (weekNo * NumberMilliSecondsWeek)));
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(1980, 0, 6, 0, 0, 0);
		long GPSEpoch = (cal.getTimeInMillis() / 1000) * 1000;
		// GPSEpoch = Math.round((double) GPSEpoch / 1000) * 1000;
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
