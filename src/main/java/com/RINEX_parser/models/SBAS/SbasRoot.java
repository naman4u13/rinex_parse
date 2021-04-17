package com.RINEX_parser.models.SBAS;

import java.util.Calendar;

import com.RINEX_parser.utility.Time;

public class SbasRoot {

	// GPS time system timestamp in UTC zone
	private Calendar time;
	// GPS time in UTC zone
	private long GPStime;
	private final long NumberSecondsWeek = 604800;

	public Calendar getTime() {
		return time;
	}

	public void setTime(Calendar time) {
		this.time = time;
	}

	public long getGPStime() {
		return GPStime;
	}

	public void setGPStime(long GPStime) {
		this.GPStime = GPStime;
	}

	public SbasRoot(Calendar time) {
		super();
		this.time = time;
		GPStime = Time.getGPSTime(time)[0];
	}

}
