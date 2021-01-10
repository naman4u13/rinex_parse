package com.RINEX_parser.models;

public class SatelliteModel {

	private int SVID;
	private double pseudorange;
	private double CNo;

	public int getSVID() {
		return SVID;
	}

	public double getPseudorange() {
		return pseudorange;
	}

	public SatelliteModel(String SVID, String pseudorange, String CNo) {
		this.SVID = Integer.parseInt(SVID.replaceAll("[A-Z]", ""));
		this.pseudorange = Double.parseDouble(pseudorange);
		this.CNo = Double.parseDouble(CNo);
	}

	public SatelliteModel(int SVID, double pseudorange, double CNo) {
		this.SVID = SVID;
		this.pseudorange = pseudorange;
		this.CNo = CNo;
	}

	@Override
	public String toString() {
		return "SatelliteModel [SVID=" + SVID + ", pseudorange=" + pseudorange + ", CNo=" + CNo + "]";
	}

	public double getCNo() {
		return CNo;
	}

	public void setCNo(double cNo) {
		CNo = cNo;
	}
}
