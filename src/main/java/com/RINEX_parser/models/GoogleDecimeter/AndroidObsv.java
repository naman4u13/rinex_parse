package com.RINEX_parser.models.GoogleDecimeter;

public class AndroidObsv {

	private double biasUnc;
	private int adrState;
	private double adrM;
	private double adrUncM;
	private double multipathInd;
	private long chipsetElapsedRealtimeNanos;
	private double prRateUncMps;
	private int hardClkDiscCount;
	boolean LLI = false;

	public AndroidObsv(String biasUnc, String adrState, String adrM, String adrUncM, String multipathInd,
			String chipsetElapsedRealtimeNanos, String prRateUncMps, String HardClkDiscCount) {
		super();
		this.biasUnc = Double.parseDouble(biasUnc) / 1e9;
		this.adrState = Integer.parseInt(adrState);
		this.adrM = Double.parseDouble(adrM);
		this.adrUncM = Double.parseDouble(adrUncM);
		this.multipathInd = Integer.parseInt(multipathInd);
		this.chipsetElapsedRealtimeNanos = Long.parseLong(chipsetElapsedRealtimeNanos);
		this.prRateUncMps = Double.parseDouble(prRateUncMps);
		this.hardClkDiscCount = Integer.parseInt(HardClkDiscCount);
		setLLI();
	}

	public double getBiasUnc() {
		return biasUnc;
	}

	public int getAdrState() {
		return adrState;
	}

	public double getAdrM() {
		return adrM;
	}

	public double getAdrUncM() {
		return adrUncM;
	}

	public double getMultipathInd() {
		return multipathInd;
	}

	public long getChipsetElapsedRealtimeNanos() {
		return chipsetElapsedRealtimeNanos;
	}

	private void setLLI() {

		StringBuffer adrStateStr = new StringBuffer(
				String.format("%" + 8 + "s", Integer.toBinaryString(adrState)).replaceAll(" ", "0")).reverse();

		if (adrStateStr.charAt(0) != '1' || adrStateStr.charAt(1) == '1' || adrStateStr.charAt(2) == '1') {
			this.LLI = true;
		}

	}

	public boolean LLI() {
		return LLI;
	}

	public double getPrRateUncMps() {
		return prRateUncMps;
	}

	public int getHardClkDiscCount() {
		return hardClkDiscCount;
	}

}
