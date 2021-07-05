package com.RINEX_parser.models.GoogleDecimeter;

public class Derived {
	private final static double SpeedofLight = 299792458;
	private double tSV;
	private double[] satECEF;
	private double[] satVel;
	private double satClkBias;
	private double satClkDriftMps;
	private double rawPrM;
	private double rawPrUncM;
	private double isrbM;
	private double ionoDelayM;
	private double tropoDelayM;

	public Derived(String[] data) {
		this.tSV = Double.parseDouble(data[0]) * 1e-9;
		this.satECEF = new double[] { Double.parseDouble(data[1]), Double.parseDouble(data[2]),
				Double.parseDouble(data[3]) };
		this.satVel = new double[] { Double.parseDouble(data[4]), Double.parseDouble(data[5]),
				Double.parseDouble(data[6]) };
		this.satClkBias = Double.parseDouble(data[7]) / SpeedofLight;
		this.satClkDriftMps = Double.parseDouble(data[8]);
		this.rawPrM = Double.parseDouble(data[9]);
		this.rawPrUncM = Double.parseDouble(data[10]);
		this.isrbM = Double.parseDouble(data[11]);
		this.ionoDelayM = Double.parseDouble(data[12]);
		this.tropoDelayM = Double.parseDouble(data[13]);
	}

	public double[] getSatECEF() {
		return satECEF;
	}

	public double[] getSatVel() {
		return satVel;
	}

	public double getSatClkBias() {
		return satClkBias;
	}

	public double getSatClkBiasMps() {
		return satClkDriftMps;
	}

	public double getRawPrM() {
		return rawPrM;
	}

	public double getRawPrUncM() {
		return rawPrUncM;
	}

	public double getIsrbM() {
		return isrbM;
	}

	public double getIonoDelayM() {
		return ionoDelayM;
	}

	public double getTropoDelayM() {
		return tropoDelayM;
	}

	public double gettSV() {
		return tSV;
	}
}
