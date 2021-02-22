package com.RINEX_parser.ComputeUserPos.Regression;

import java.util.ArrayList;
import java.util.stream.IntStream;

import com.RINEX_parser.ComputeUserPos.Regression.Models.LinearLeastSquare;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;

public class WLS extends LinearLeastSquare {

	public WLS(ArrayList<Satellite> SV, IonoCoeff ionoCoeff) {
		super(SV, ionoCoeff);

		setWeight(SV);

	}

	public WLS(ArrayList<Satellite> SV) {
		super(SV);

		setWeight(SV);
	}

	@Override
	public double[] getEstECEF() {
		estimate(getPR());
		System.out.println("\nWGDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();

	}

	public double[] getIonoCorrECEF() {

		estimate(getIonoCorrPR());
		System.out.println("Iono WGDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();
	}

	// Sarab Tay and Juliette Marais -
	// https://www.researchgate.net/publication/260200581_Weighting_models_for_GPS_Pseudorange_observations_for_land_transportation_in_urban_canyons
	// Hybrid WLS estimator combines both Carrier to Noise ratio and Elevation angle
	// info to compute weighing matrix, Intial results show its the superior
	// estimator in comparison to standalone estimator based on CNo or Elev angle.
	public static double computeCoVariance(double CNo, double ElevAng) {
		double var = Math.pow(10, -(CNo / 10)) / Math.pow(Math.sin(ElevAng), 2);
		return var;
	}

	public static double[][] normalize(double[][] W) {
		double sum = 0;
		int n = W.length;
		for (int i = 0; i < W.length; i++) {
			sum += W[i][i];
		}
		final double fSum = sum;
		IntStream.range(0, n).forEach(i -> W[i][i] = W[i][i] / fSum);
		return W;
	}

	public void setWeight(ArrayList<Satellite> SV) {
		int SVcount = SV.size();
		ArrayList<double[]> AzmEle = getAzmEle();
		double[][] Weight = new double[SVcount][SVcount];
		IntStream.range(0, SVcount)
				.forEach(i -> Weight[i][i] = 1 / computeCoVariance(SV.get(i).getCNo(), AzmEle.get(i)[0]));
		super.setWeight(Weight);
	}

}
