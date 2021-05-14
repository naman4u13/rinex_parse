package com.RINEX_parser.models;

public class Observable {

	private static final double SPEED_OF_LIGHT = 299792458;
	private final double carrier_frequency;
	private final double carrier_wavelength;
	private int SVID;
	private double pseudorange;
	private double CNo;
	private double doppler;
	private double pseudoRangeRate;
	private double phase;

	public int getSVID() {
		return SVID;
	}

	public double getPseudorange() {
		return pseudorange;
	}

	public Observable(String SVID, String pseudorange, String CNo, String doppler, String phase,
			String carrier_frequency) {
		this.SVID = Integer.parseInt(SVID.replaceAll("[A-Z]", ""));
		this.pseudorange = pseudorange != null ? Double.parseDouble(pseudorange) : 0;
		this.CNo = CNo != null ? Double.parseDouble(CNo) : 0;
		this.doppler = doppler != null ? Double.parseDouble(doppler) : 0;
		this.phase = phase != null ? Double.parseDouble(phase) : 0;
		this.carrier_frequency = Double.parseDouble(carrier_frequency);
		this.carrier_wavelength = SPEED_OF_LIGHT / this.carrier_frequency;
		this.pseudoRangeRate = -this.doppler * this.carrier_wavelength;
	}

	public Observable(int SVID, double pseudorange, double CNo, double doppler, double phase,
			double carrier_frequency) {
		this.SVID = SVID;
		this.pseudorange = pseudorange;
		this.CNo = CNo;
		this.doppler = doppler;
		this.phase = phase;
		this.carrier_frequency = carrier_frequency;
		this.carrier_wavelength = SPEED_OF_LIGHT / this.carrier_frequency;
		this.pseudoRangeRate = -this.doppler * this.carrier_wavelength;
	}

	public Observable(Observable satModel) {
		this.SVID = satModel.getSVID();
		this.pseudorange = satModel.getPseudorange();
		this.CNo = satModel.getCNo();
		this.doppler = satModel.getDoppler();
		this.phase = satModel.phase;
		this.carrier_frequency = satModel.carrier_frequency;
		this.carrier_wavelength = satModel.carrier_wavelength;
		this.pseudoRangeRate = satModel.pseudoRangeRate;
	}

	@Override
	public String toString() {
		return "Observable [SVID=" + SVID + ", pseudorange=" + pseudorange + ", CNo=" + CNo + ", doppler=" + doppler
				+ ", pseudoRangeRate=" + pseudoRangeRate + "]";
	}

	public double getCNo() {
		return CNo;
	}

	public void setCNo(double cNo) {
		CNo = cNo;
	}

	public double getDoppler() {
		return doppler;
	}

	public void setDoppler(double doppler) {
		this.doppler = doppler;
	}

	public void setSVID(int sVID) {
		SVID = sVID;
	}

	public void setPseudorange(double pseudorange) {
		this.pseudorange = pseudorange;
	}

	public double getPseudoRangeRate() {
		return pseudoRangeRate;
	}

	public void setPseudoRangeRate(double pseudoRangeRate) {
		this.pseudoRangeRate = pseudoRangeRate;
	}
}
