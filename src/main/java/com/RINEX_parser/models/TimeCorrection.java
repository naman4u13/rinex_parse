package com.RINEX_parser.models;

public class TimeCorrection {

	private double a0;
	private double a1;
	private long tRef;

	@Override
	public String toString() {
		return "TimeCorrection [a0=" + a0 + ", a1=" + a1 + ", tRef=" + tRef + ", weekNo=" + weekNo + ", leapSeconds="
				+ leapSeconds + "]";
	}

	private long weekNo;
	private long leapSeconds;

	public void setGPUT(String[] GPUT) {
		a0 = Double.parseDouble(GPUT[0]);
		a1 = Double.parseDouble(GPUT[1]);
		tRef = Long.parseLong(GPUT[2]);
		weekNo = Long.parseLong(GPUT[3]);
	}

	public void setLeapSeconds(long leaspSeconds) {
		this.leapSeconds = leaspSeconds;
	}

	public double getA0() {
		return a0;
	}

	public double getA1() {
		return a1;
	}

	public long gettRef() {
		return tRef;
	}

	public long getWeekNo() {
		return weekNo;
	}

	public long getLeapSeconds() {
		return leapSeconds;
	}
}
