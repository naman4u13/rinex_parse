package com.RINEX_parser.ComputeUserPos.Regression;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.stream.IntStream;

import com.RINEX_parser.ComputeUserPos.Regression.Models.DopplerLLS;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.Weight;

public class Doppler extends DopplerLLS {
	public Doppler(ArrayList<Satellite> SV, IonoCoeff ionoCoeff, Calendar time) {
		super(SV, ionoCoeff, time);

		setWeight(SV);

	}

	public double[] getEstECEF(boolean isStatic) {
		estimate(getPR(), isStatic);
		System.out.println("\nPDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();

	}

	public double[] getIonoCorrECEF(boolean isStatic) {
		estimate(getIonoCorrPR(), isStatic);
		System.out.println("Iono WPDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();
	}

	public void setWeight(ArrayList<Satellite> SV) {
		int SVcount = SV.size();
		ArrayList<double[]> AzmEle = getAzmEle();
		double[][] weight = new double[2 * SVcount][2 * SVcount];
		IntStream.range(0, SVcount).forEach(i -> {
			weight[i][i] = 1 / Weight.computeCoVariance(SV.get(i).getCNo(), AzmEle.get(i)[0]);
			weight[SVcount + i][SVcount + i] = weight[i][i];
		});
		double[][] normWeight = Weight.normalize(weight);
		// for Pseudorange measurement error is 10 meter and for Doppler it is 0.1 meter
		IntStream.range(0, SVcount).forEach(i -> {
			normWeight[i][i] = normWeight[i][i] / 100;
			normWeight[SVcount + i][SVcount + i] = normWeight[SVcount + i][SVcount + i] / 0.01;
		});
		super.setWeight(normWeight);
	}

	// Weight matrix for ComputeRcvrInfo is different
	@Override
	public void computeRcvrInfo(boolean isStatic) {
		// TODO Auto-generated method stub
		super.setWeight(Weight.computeWeight(getSV(), getTime()));
		super.computeRcvrInfo(isStatic);
	}

	@Override
	public void computeRcvrInfo(double[] userECEF, boolean isStatic) {
		// TODO Auto-generated method stub
		super.setWeight(Weight.computeWeight(getSV(), getTime()));
		super.computeRcvrInfo(userECEF, isStatic);
	}

}
