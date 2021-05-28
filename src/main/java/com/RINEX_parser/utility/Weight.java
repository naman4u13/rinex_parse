package com.RINEX_parser.utility;

import java.util.ArrayList;
import java.util.stream.IntStream;

import com.RINEX_parser.ComputeUserPos.Regression.Models.LinearLeastSquare;
import com.RINEX_parser.models.Satellite;

public class Weight {

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

		int n = W.length;
		double rms = Math
				.sqrt(IntStream.range(0, n).mapToDouble(i -> W[i][i] * W[i][i]).reduce(0, (i, j) -> i + j) / n);

		IntStream.range(0, n).forEach(i -> W[i][i] = W[i][i] / rms);
		return W;
	}

	public static double[][] computeWeight(LinearLeastSquare lls) {

		ArrayList<double[]> AzmEle = lls.getAzmEle();
		int SVcount = AzmEle.size();
		ArrayList<Satellite> SV = lls.getSV();
		double[][] weight = new double[SVcount][SVcount];
		IntStream.range(0, SVcount)
				.forEach(i -> weight[i][i] = 1 / Weight.computeCoVariance(SV.get(i).getCNo(), AzmEle.get(i)[0]));
		double[][] normWeight = Weight.normalize(weight);
		return normWeight;
	}

	public static double[][] computeWeight(ArrayList<Satellite>[] SV, ArrayList<double[]> AzmEle) {

		int SVcount = AzmEle.size();
		double[][] weight = new double[2 * SVcount][2 * SVcount];
		IntStream.range(0, SVcount)
				.forEach(i -> weight[i][i] = 1 / Weight.computeCoVariance(SV[0].get(i).getCNo(), AzmEle.get(i)[0]));
		IntStream.range(SVcount, 2 * SVcount).forEach(i -> weight[i][i] = 1
				/ Weight.computeCoVariance(SV[1].get(i - SVcount).getCNo(), AzmEle.get(i - SVcount)[0]));
		double[][] normWeight = Weight.normalize(weight);
		return normWeight;

	}

	public static double[][] computeIdentityMat(int SVcount) {
		double[][] Weight = new double[SVcount][SVcount];
		IntStream.range(0, SVcount).forEach(i -> Weight[i][i] = 1);
		return Weight;
	}

}
