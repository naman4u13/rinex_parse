package com.RINEX_parser.models.SBAS;

public class SbasRoot {

	// GPS time in UTC zone
	private long GPSTime;

	private long weekNo;

	public long getGPStime() {
		return GPSTime;
	}

	public long getWeekNo() {
		return weekNo;
	}

	public void setGPStime(long GPSTime, long weekNo) {
		this.GPSTime = GPSTime;
		this.weekNo = weekNo;

	}

	public SbasRoot(long GPSTime, long weekNo) {
		super();
		this.GPSTime = GPSTime;
		this.weekNo = weekNo;
	}

}
