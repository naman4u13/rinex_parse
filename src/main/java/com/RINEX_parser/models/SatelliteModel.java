package com.RINEX_parser.models;

public class SatelliteModel {

	private int SVID;
	private double pseudorange;

	public int getSVID() {
		return SVID;
	}

	public double getPseudorange() {
		return pseudorange;
	}

	public SatelliteModel(String SVID, String pseudorange) {
		this.SVID = Integer.parseInt(SVID.replaceAll("[A-Z]", ""));
		this.pseudorange = Double.parseDouble(pseudorange);
	}

	public SatelliteModel(int SVID, double pseudorange) {
		this.SVID = SVID;
		this.pseudorange = pseudorange;
	}

	@Override
	public String toString() {
		return "SatelliteModel [SVID=" + SVID + ", pseudorange=" + pseudorange + "]";
	}
}
