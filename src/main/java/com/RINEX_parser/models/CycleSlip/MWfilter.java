package com.RINEX_parser.models.CycleSlip;

import java.util.ArrayList;
import java.util.List;

public class MWfilter {

	private double mean = 0;
	private double sigmaSq = 0;
	private double mean300 = 0;
	private List<LinearCombo> mwList = null;
	private int n300 = 0;

	public MWfilter(int samplingRate) {
		mwList = new ArrayList<LinearCombo>();
		this.n300 = 300 / samplingRate;

	}

	public double getMean() {
		return mean;
	}

	public double getSigmaSq() {
		return sigmaSq;
	}

	public double getMean300() {
		return mean300;
	}

	public List<LinearCombo> getMwList() {
		return mwList;
	}

	public void update(double mw, long t) {
		// the position of new element to be added
		int k = mwList.size() + 1;

		if (k > n300) {
			// Get first element to subtract
			double val = mwList.get(mwList.size() - n300).lc() / n300;
			mean300 = mean300 - val + (mw / n300);

		} else {
			mean300 = smooth(mean300, mw, k);
		}

		sigmaSq = smooth(sigmaSq, Math.pow(mw - mean, 2), k);
		mean = smooth(mean, mw, k);
		mwList.add(new LinearCombo(mw, t));

	}

	private double smooth(double m, double val, double k) {
		m = (((k - 1) / k) * m) + ((1 / k) * val);
		return m;
	}

}
