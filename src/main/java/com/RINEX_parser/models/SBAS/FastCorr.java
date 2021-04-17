package com.RINEX_parser.models.SBAS;

import java.util.Calendar;

public class FastCorr extends SbasRoot {

	private double[] PRC;

	public FastCorr(Calendar time, double[] PRC, int[] UDREI) {
		super(time);
		// TODO Auto-generated constructor stub
		int n = PRC.length;
		this.PRC = new double[n];
		monitoredPRC(PRC, UDREI, n);

	}

	private void monitoredPRC(double[] PRC, int[] UDREI, int n) {
		for (int i = 0; i < UDREI.length; i++) {
			this.PRC[i] = PRC[i];
			// Do not use condition
			if (UDREI[i] == 15) {
				this.PRC[i] = 0.0;
			}
		}
	}

	public double[] getPRC() {
		return PRC;
	}

	public void setPRC(double[] pRC) {
		PRC = pRC;
	}

}
