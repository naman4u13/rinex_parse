package com.RINEX_parser.ComputeUserPos.Regression;

import java.util.ArrayList;
import java.util.stream.IntStream;

import com.RINEX_parser.ComputeUserPos.Regression.Models.DeltaRangeLLS;
import com.RINEX_parser.models.Satellite;

public class DeltaRange extends DeltaRangeLLS {

	public DeltaRange(ArrayList<Satellite> SV_t, ArrayList<Satellite> SV_tmin1) {

		super(SV_t, SV_tmin1);
		setWeight(SV_t.size());

	}

	@Override
	public double[] getEstECEF() {
		estimate();
		System.out.println("\nGDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();

	}

	public void setWeight(int SVcount) {
		double[][] Weight = new double[SVcount][SVcount];
		IntStream.range(0, SVcount).forEach(i -> Weight[i][i] = 1);
		super.setWeight(Weight);
	}

}
