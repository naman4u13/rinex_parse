package com.RINEX_parser.ComputeUserPos.Regression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.IntStream;

import com.RINEX_parser.ComputeUserPos.Regression.Models.DeltaRangeLLS;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.Weight;

public class DeltaRange extends DeltaRangeLLS {

	public DeltaRange(ArrayList<Satellite> SV_t, ArrayList<Satellite> SV_tmin1) {
		// Reduntant as correct SV will be set below
		super(SV_t);
		// Correct SV will now be set
		checkCompatibleSV(SV_t, SV_tmin1);

	}

	@Override
	public double[] getEstECEF() {
		estimate();
		System.out.println("\nPDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();

	}

	public void setWeight(int SVcount) {
		double[][] Weight = new double[SVcount][SVcount];
		IntStream.range(0, SVcount).forEach(i -> Weight[i][i] = 1);
		super.setWeight(Weight);
	}

	// find out common satellite present on both epochs - t and t-1, and arrange
	// them in same order
	public void checkCompatibleSV(ArrayList<Satellite> SV_t, ArrayList<Satellite> SV_tmin1) {
		ArrayList<Satellite> new_SV_t = new ArrayList<Satellite>();
		ArrayList<Satellite> new_SV_tmin1 = new ArrayList<Satellite>();
		HashMap<Integer, Satellite> map = new HashMap<Integer, Satellite>();
		HashSet<Integer> set = new HashSet<Integer>();
		SV_t.stream().forEach(i -> {
			map.put(i.getSVID(), i);
			set.add(i.getSVID());
		});

		for (Satellite sat : SV_tmin1) {
			int svid = sat.getSVID();
			if (set.contains(svid)) {
				new_SV_t.add(map.get(svid));
				new_SV_tmin1.add(sat);
			}
		}
		setSVs(new_SV_t, new_SV_tmin1);
		setWeight(new_SV_t.size());

	}

	public void setWeight(ArrayList<Satellite> SV) {
		int SVcount = SV.size();
		ArrayList<double[]> AzmEle = getAzmEle();
		double[][] weight = new double[SVcount][SVcount];
		IntStream.range(0, SVcount)
				.forEach(i -> weight[i][i] = 1 / Weight.computeCoVariance(SV.get(i).getCNo(), AzmEle.get(i)[0]));
		double[][] normWeight = Weight.normalize(weight);
		super.setWeight(normWeight);
	}

}
