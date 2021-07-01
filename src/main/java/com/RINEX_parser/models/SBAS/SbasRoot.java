package com.RINEX_parser.models.SBAS;

public class SbasRoot {

	// GPS time in UTC zone
	private double GPSTime;

	private long weekNo;

	public double getGPStime() {
		return GPSTime;
	}

	public long getWeekNo() {
		return weekNo;
	}

	public void setGPStime(double GPSTime, long weekNo) {
		this.GPSTime = GPSTime;
		this.weekNo = weekNo;

	}

	public SbasRoot(double GPSTime, long weekNo) {
		super();
		this.GPSTime = GPSTime;
		this.weekNo = weekNo;
	}

}
