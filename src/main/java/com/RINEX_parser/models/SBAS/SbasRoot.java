package com.RINEX_parser.models.SBAS;

import java.util.Calendar;

import com.RINEX_parser.utility.Time;

public class SbasRoot {

	// GPS time system timestamp in UTC zone
	private Calendar time;
	// GPS time in UTC zone
	private long GPSTime;

	public Calendar getTime() {
		return time;
	}

	public void setTime(Calendar time) {
		this.time = time;
		GPSTime = Time.getGPSTime(time)[0];
	}

	public long getGPStime() {
		return GPSTime;
	}

	public void setGPStime(long GPSTime, long weekNo) {
		this.GPSTime = GPSTime;
		this.time = Time.getDate(GPSTime, weekNo, 0);
	}

	public SbasRoot(Calendar time) {
		super();
		this.time = time;
		GPSTime = Time.getGPSTime(time)[0];
	}

}
