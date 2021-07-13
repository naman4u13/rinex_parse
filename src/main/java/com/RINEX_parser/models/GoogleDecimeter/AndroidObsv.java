package com.RINEX_parser.models.GoogleDecimeter;

import java.util.Arrays;
import java.util.HashSet;

public class AndroidObsv {

	private static final HashSet<Integer> valid_state = new HashSet<Integer>(Arrays.asList(1, 8, 16, 9, 17, 24, 25));
	private double biasUnc;
	private int adrState;
	private double adrM;
	private double adrUncM;
	private double multipathInd;
	private long chipsetElapsedRealtimeNanos;
	boolean LLI = true;

	public AndroidObsv(double biasUnc, int adrState, double adrM, double adrUncM, double multipathInd,
			long chipsetElapsedRealtimeNanos) {
		super();
		this.biasUnc = biasUnc;
		this.adrState = adrState;
		this.adrM = adrM;
		this.adrUncM = adrUncM;
		this.multipathInd = multipathInd;
		this.chipsetElapsedRealtimeNanos = chipsetElapsedRealtimeNanos;
		setLLI();
	}

	public AndroidObsv(String biasUnc, String adrState, String adrM, String adrUncM, String multipathInd,
			String chipsetElapsedRealtimeNanos) {
		super();
		this.biasUnc = Double.parseDouble(biasUnc) / 1e9;
		this.adrState = Integer.parseInt(adrState);
		this.adrM = Double.parseDouble(adrM);
		this.adrUncM = Double.parseDouble(adrUncM);
		this.multipathInd = Integer.parseInt(multipathInd);
		this.chipsetElapsedRealtimeNanos = Long.parseLong(chipsetElapsedRealtimeNanos);
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
		this.LLI = (!valid_state.contains(this.adrState));
	}

}
