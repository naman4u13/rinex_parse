package com.RINEX_parser.models.GoogleDecimeter;

public class Derived {
	private final static double SpeedofLight = 299792458;
	private final static long NumberNanoSecondsWeek = (long) (604800 * 1e9);
	private long tSV;
	private double[] satECEF;
	private double[] satVel;
	private double satClkBias;
	private double satClkDrift;
	private double rawPrM;
	private double rawPrUncM;
	private double isrbM;
	private double ionoDelayM;
	private double tropoDelayM;

	public Derived(String[] data) {

		this.tSV = ((Long.parseLong(data[0])) % NumberNanoSecondsWeek);
		this.satECEF = new double[] { Double.parseDouble(data[1]), Double.parseDouble(data[2]),
				Double.parseDouble(data[3]) };
		this.satVel = new double[] { Double.parseDouble(data[4]), Double.parseDouble(data[5]),
				Double.parseDouble(data[6]) };
		this.satClkBias = Double.parseDouble(data[7]) / SpeedofLight;
		this.satClkDrift = Double.parseDouble(data[8]) / SpeedofLight;
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

	public double getSatClkDrift() {
		return satClkDrift;
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

	public long gettSV() {
		return tSV;
	}
}
