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
	private double cycle;
	private boolean isLocked;
	private char SSI;
	private boolean LLI = true;

	public int getSVID() {
		return SVID;
	}

	public double getPseudorange() {
		return pseudorange;
	}

	public Observable(char SSI, String SVID, String pseudorange, String CNo, String doppler, String phase,
			String carrier_frequency) {
		this.SSI = SSI;
		this.SVID = Integer.parseInt(SVID.replaceAll("[A-Z]", ""));
		this.pseudorange = pseudorange != null ? Double.parseDouble(parseObs(pseudorange, true)) : 0;
		this.CNo = CNo != null ? Double.parseDouble(parseObs(CNo)) : 0;
		this.doppler = doppler != null ? Double.parseDouble(parseObs(doppler)) : 0;
		this.cycle = phase != null ? Double.parseDouble(parseObs(phase)) : 0;

		this.carrier_frequency = Double.parseDouble(carrier_frequency);
		this.carrier_wavelength = SPEED_OF_LIGHT / this.carrier_frequency;
		this.pseudoRangeRate = -this.doppler * this.carrier_wavelength;
		this.isLocked = false;

	}

	public Observable(Observable satModel) {
		this.SSI = satModel.getSSI();
		this.SVID = satModel.getSVID();
		this.pseudorange = satModel.getPseudorange();
		this.CNo = satModel.getCNo();
		this.doppler = satModel.getDoppler();
		this.cycle = satModel.cycle;
		this.carrier_frequency = satModel.carrier_frequency;
		this.carrier_wavelength = satModel.carrier_wavelength;
		this.pseudoRangeRate = satModel.pseudoRangeRate;
		this.isLocked = satModel.isLocked;
		this.LLI = satModel.LLI();
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

	public double getCarrier_frequency() {
		return carrier_frequency;
	}

	public double getCycle() {
		return cycle;
	}

	public double getCarrier_wavelength() {
		return carrier_wavelength;
	}

	public boolean isLocked() {
		return isLocked;
	}

	public void setLocked(boolean isLocked) {
		this.isLocked = isLocked;
	}

	public char getSSI() {
		return SSI;
	}

	// Observation data format is F14.3,I1,I1 last
	public String parseObs(String data) {
		return parseObs(data, false);
	}

	// Observation data format is F14.3,I1,I1 last
	public String parseObs(String data, boolean setLLI) {

		if (data != null) {
			if (!data.isBlank()) {
				String[] arr = data.split("\\.");

				if (arr[1].length() > 3) {
					data = arr[0] + "." + arr[1].substring(0, 3);
					if (setLLI) {
						double val = Double.parseDouble(String.valueOf(arr[1].charAt(3)));
						if (val % 2 == 0) {
							// is either 0,2,4,6 as bit 0 is not set
							LLI = false;
						}
					}
				} else {
					data = arr[0] + "." + arr[1];

				}

			}

		}
		return data;

	}

	public boolean LLI() {
		return LLI;
	}

	public void setCycle(double cycle) {
		this.cycle = cycle;
	}

}
