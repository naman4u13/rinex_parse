package com.RINEX_parser.ComputeUserPos.Regression;

import java.util.ArrayList;
import java.util.stream.IntStream;

import com.RINEX_parser.ComputeUserPos.Regression.Models.DopplerLLS;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;

public class Doppler extends DopplerLLS {
	public Doppler(ArrayList<Satellite> SV, IonoCoeff ionoCoeff) {
		super(SV, ionoCoeff);

		setWeight(SV);

	}

	public double[] getEstECEF(boolean isStatic) {
		estimate(getPR(), isStatic);
		System.out.println("\nWGDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();

	}

	public double[] getIonoCorrECEF(boolean isStatic) {
		estimate(getIonoCorrPR(), isStatic);
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
		double[][] Weight = new double[2 * SVcount][2 * SVcount];
		IntStream.range(0, SVcount).forEach(i -> {
			Weight[i][i] = 1 / computeCoVariance(SV.get(i).getCNo(), AzmEle.get(i)[0]);
			Weight[SVcount + i][SVcount + i] = Weight[i][i];
		});
		double[][] normWeight = normalize(Weight);
		// for Pseudorange measurement error is 10 meter and for Doppler it is 0.1 meter
		IntStream.range(0, SVcount).forEach(i -> {
			normWeight[i][i] = normWeight[i][i] / 100;
			Weight[SVcount + i][SVcount + i] = Weight[SVcount + i][SVcount + i] / 0.01;
		});
		super.setWeight(normWeight);
	}

}
