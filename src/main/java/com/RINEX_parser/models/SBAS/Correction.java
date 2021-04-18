package com.RINEX_parser.models.SBAS;

import java.util.HashMap;

public class Correction {

	private FastCorr FC;
	private HashMap<Integer, LongTermCorr> LTC;

	public Correction() {
		this.LTC = new HashMap<Integer, LongTermCorr>();
	}

	public FastCorr getFC() {
		return FC;
	}

	public void setFC(FastCorr fC) {
		FC = fC;
	}

	public HashMap<Integer, LongTermCorr> getLTC() {
		return LTC;
	}

	public void setLTC(int IODE, LongTermCorr ltc) {
		LTC.put(IODE, ltc);
	}

}
