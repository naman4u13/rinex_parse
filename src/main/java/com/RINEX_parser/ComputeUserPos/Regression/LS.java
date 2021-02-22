package com.RINEX_parser.ComputeUserPos.Regression;

import java.util.ArrayList;
import java.util.stream.IntStream;

import com.RINEX_parser.ComputeUserPos.Regression.Models.LinearLeastSquare;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;

public class LS extends LinearLeastSquare {

	public LS(ArrayList<Satellite> SV, IonoCoeff ionoCoeff) {
		super(SV, ionoCoeff);
		int SVcount = SV.size();
		setWeight(SVcount);

	}

	public LS(ArrayList<Satellite> SV) {
		super(SV);
		int SVcount = SV.size();
		setWeight(SVcount);
	}

	@Override
	public double[] getEstECEF() {
		estimate(getPR());
		System.out.println("\nGDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();

	}

	public double[] getIonoCorrECEF() {

		estimate(getIonoCorrPR());
		System.out.println("Iono GDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();
	}

	public void setWeight(int SVcount) {
		double[][] Weight = new double[SVcount][SVcount];
		IntStream.range(0, SVcount).forEach(i -> Weight[i][i] = 1);
		super.setWeight(Weight);
	}
}
