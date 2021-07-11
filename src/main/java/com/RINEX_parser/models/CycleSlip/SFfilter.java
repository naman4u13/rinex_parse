package com.RINEX_parser.models.CycleSlip;

import java.util.ArrayList;
import java.util.List;

public class SFfilter {

	private double mean = 0;

	private double meanSq = 0;
	private int n;
	private List<LinearCombo> dList = null;
	private double s0Sq;

	public SFfilter(int samplingRate) {
		dList = new ArrayList<LinearCombo>();
		n = 300 / samplingRate;
		s0Sq = Math.pow(3, 2);
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

	public double getSigmaSq() {
		double sigmaSq = meanSq - Math.pow(mean, 2);
		double len = dList.size();
		sigmaSq = (((len - 1) / len) * sigmaSq) + ((1 / len) * s0Sq);
		return sigmaSq;

	}

	public SFfilter reset(int samplingRate, int minAL_SF) {

		SFfilter tempSFfilter = new SFfilter(samplingRate);
		if (dList.size() == minAL_SF) {
			for (int i = 1; i < minAL_SF; i++) {
				LinearCombo obj = dList.get(i);
				tempSFfilter.update(obj.lc(), obj.t());
			}
		}

		return tempSFfilter;

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
