package com.RINEX_parser.models;

public class SatelliteModel {

	private final double SpeedofLight = 299792458;
	private final double L1_frequency = 1575.42E6;
	private final double L1_wavelength = SpeedofLight / L1_frequency;
	private int SVID;
	private double pseudorange;
	private double CNo;
	private double doppler;
	private double PseudoRangeRate;

	public int getSVID() {
		return SVID;
	}

	public double getPseudorange() {
		return pseudorange;
	}

	public SatelliteModel(String SVID, String pseudorange, String CNo, String doppler) {
		this.SVID = Integer.parseInt(SVID.replaceAll("[A-Z]", ""));
		this.pseudorange = Double.parseDouble(pseudorange);
		this.CNo = Double.parseDouble(CNo);
		this.doppler = Double.parseDouble(doppler);
		this.PseudoRangeRate = -this.doppler * L1_wavelength;
	}

	public SatelliteModel(int SVID, double pseudorange, double CNo, double doppler) {
		this.SVID = SVID;
		this.pseudorange = pseudorange;
		this.CNo = CNo;
		this.doppler = doppler;
		this.PseudoRangeRate = -this.doppler * L1_wavelength;
	}

	public SatelliteModel(SatelliteModel satModel) {
		this.SVID = satModel.getSVID();
		this.pseudorange = satModel.getPseudorange();
		this.CNo = satModel.getCNo();
		this.doppler = satModel.getDoppler();
		this.PseudoRangeRate = -this.doppler * L1_wavelength;
	}

	@Override
	public String toString() {
		return "SatelliteModel [SVID=" + SVID + ", pseudorange=" + pseudorange + ", CNo=" + CNo + ", doppler=" + doppler
				+ ", PseudoRangeRate=" + PseudoRangeRate + "]";
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
		return PseudoRangeRate;
	}

	public void setPseudoRangeRate(double pseudoRangeRate) {
		PseudoRangeRate = pseudoRangeRate;
	}
}
