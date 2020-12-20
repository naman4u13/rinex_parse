package com.RINEX_parser.models;

import java.util.ArrayList;

import com.RINEX_parser.utility.Time;

public class ObservationMsg {

	private double x;
	private double y;
	private double z;
	private int year;
	private int month;
	private int day;
	private int hour;
	private int minute;
	private double second;
	private int ObsvSatCount;
	private ArrayList<SatelliteModel> obsvSat = new ArrayList<SatelliteModel>();
	private long tRX;
	private long weekNo;

	public void setObsvSat(ArrayList<SatelliteModel> obsvSat) {
		this.obsvSat = obsvSat;
		ObsvSatCount = obsvSat.size();
	}

	public void set_ECEF_XYZ(double[] ECEF_XYZ) {
		x = ECEF_XYZ[0];
		y = ECEF_XYZ[1];
		z = ECEF_XYZ[2];
	}

	public void set_RxTime(String[] RxTime) {
		year = Integer.parseInt(RxTime[0]);
		month = Integer.parseInt(RxTime[1]);
		day = Integer.parseInt(RxTime[2]);
		hour = Integer.parseInt(RxTime[3]);
		minute = Integer.parseInt(RxTime[4]);
		second = Double.parseDouble(RxTime[5]);
		long[] GPStime = Time.getGPSTime(year, month - 1, day, hour, minute, (int) second);
		tRX = GPStime[0];
		weekNo = GPStime[1];
	}

	public int getYear() {
		return year;
	}

	public int getObsvSatCount() {
		return ObsvSatCount;
	}

	public void setObsvSatCount(int obsvSatCount) {
		ObsvSatCount = obsvSatCount;
	}

	public int getMonth() {
		return month;
	}

	public int getDay() {
		return day;
	}

	public int getHour() {
		return hour;
	}

	public int getMinute() {
		return minute;
	}

	public double getSecond() {
		return second;
	}

	public double[] getECEF() {
		return new double[] { x, y, z };
	}

	@Override
	public String toString() {
		return "ObservationMsg [x=" + x + ", y=" + y + ", z=" + z + ", year=" + year + ", month=" + month + ", day="
				+ day + ", hour=" + hour + ", minute=" + minute + ", second=" + second + ", ObsvSatCount="
				+ ObsvSatCount + ", obsvSat=" + obsvSat + "]";
	}

	public ArrayList<SatelliteModel> getObsvSat() {
		return obsvSat;
	}

	public long getTRX() {
		return tRX;
	}

	public long getWeekNo() {
		return weekNo;
	}
}
