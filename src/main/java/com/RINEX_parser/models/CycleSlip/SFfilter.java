package com.RINEX_parser.models.CycleSlip;

import java.util.ArrayList;
import java.util.List;

public class SFfilter {

	private double mean = 0;

	private double meanSq = 0;
	private int n;
	private List<LinearCombo> dList = null;

	public SFfilter(int samplingRate) {
		dList = new ArrayList<LinearCombo>();
		n = 300 / samplingRate;
	}

	public void update(double d, double t) {
		// the position of new element to be added
		int k = dList.size() + 1;

		if (k > n) {
			// Get first element to subtract
			double val = dList.get(dList.size() - n).lc() / n;
			mean = mean - val + (d / n);
			meanSq = meanSq - (val * val * n) + ((d * d) / n);

		} else {
			mean = smooth(mean, d, k);
			meanSq = smooth(meanSq, d * d, k);
		}

		dList.add(new LinearCombo(d, t));
	}

	public double getSigma() {
		double sigma = Math.sqrt(meanSq - Math.pow(mean, 2));
		return sigma;

	}

	private double smooth(double m, double val, double k) {
		m = (((k - 1) / k) * m) + ((1 / k) * val);
		return m;
	}

	public double getMean() {
		return mean;
	}

	public double getMeanSq() {
		return meanSq;
	}

	public List<LinearCombo> getdList() {
		return dList;
	}

}
