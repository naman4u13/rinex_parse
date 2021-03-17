package com.RINEX_parser.ComputeUserPos.Regression.Models;

import java.util.ArrayList;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;

import com.RINEX_parser.models.Satellite;

public class DeltaRangeLLS extends LinearLeastSquare {
	private final static double SpeedofLight = 299792458;
	private ArrayList<Satellite> SV_t;
	private ArrayList<Satellite> SV_tmin1;

	public DeltaRangeLLS(ArrayList<Satellite> SV_t) {

		super(SV_t);

		// TODO Auto-generated constructor stub
	}

	public void setSVs(ArrayList<Satellite> SV_t, ArrayList<Satellite> SV_tmin1) {
		this.SV_t = SV_t;
		this.SV_tmin1 = SV_tmin1;
		setSV(SV_t);
	}

	public void estimate() {

		estimate(getWeight());
	}

	public void estimate(double[][] Weight) {
		double[] estECEF = new double[] { 0, 0, 0 };
		SimpleMatrix HtWHinv = null;
		double approxRcvrClkDrift = 0;
		int SVcount = SV_t.size();

		double error = Double.MAX_VALUE;
		// Get Millimeter Accuracy, actually it takes atleast 5 iterations to converge
		// to the result which is accurate more than micrometer scale
		double threshold = 1e-3;

		if (SVcount >= 4) {

			while (error >= threshold) {
				double[][] delta_DeltaRange = new double[SVcount][1];

				double[][] H_t = new double[SVcount][3];
				double[][] H_tmin1 = new double[SVcount][3];
				for (int i = 0; i < SVcount; i++) {

					Satellite sat_t = SV_t.get(i);
					Satellite sat_tmin1 = SV_tmin1.get(i);

					double[] satECEF_t = sat_t.getECEF();
					double[] satECEF_tmin1 = sat_tmin1.getECEF();

					double approxGR_t = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> satECEF_t[x] - estECEF[x])
							.map(x -> Math.pow(x, 2)).reduce((x, y) -> x + y).getAsDouble());
					double approxGR_tmin1 = Math
							.sqrt(IntStream.range(0, 3).mapToDouble(x -> satECEF_tmin1[x] - estECEF[x])
									.map(x -> Math.pow(x, 2)).reduce((x, y) -> x + y).getAsDouble());

					double PRrate_t = sat_t.getPseudoRangeRate() + (SpeedofLight * sat_t.getSatClkDrift());
					double PRrate_tmin1 = sat_tmin1.getPseudoRangeRate() + (SpeedofLight * sat_tmin1.getSatClkDrift());
					double deltaRange = 0.5 * 1 * (PRrate_t + PRrate_tmin1);
					// No need to add approx rcvr clk info as it will get cancelled, because we are
					// considering constant for consecutive epochs
					delta_DeltaRange[i][0] = deltaRange - (approxGR_t - approxGR_tmin1);

					H_t[i] = IntStream.range(0, 3).mapToDouble(j -> -(satECEF_t[j] - estECEF[j]) / approxGR_t)
							.toArray();
					H_tmin1[i] = IntStream.range(0, 3)
							.mapToDouble(j -> -(satECEF_tmin1[j] - estECEF[j]) / approxGR_tmin1).toArray();

				}
				SimpleMatrix H;
				H = new SimpleMatrix(H_t).minus(new SimpleMatrix(H_tmin1));
				SimpleMatrix unitVector = new SimpleMatrix(SVcount, 1);
				unitVector.fill(1.0);
				H = H.concatColumns(unitVector);
				SimpleMatrix Ht = H.transpose();
				SimpleMatrix W = new SimpleMatrix(Weight);

				HtWHinv = (Ht.mult(W).mult(H)).invert();

				SimpleMatrix DeltaY = new SimpleMatrix(delta_DeltaRange);
				SimpleMatrix DeltaX = HtWHinv.mult(Ht).mult(W).mult(DeltaY);
				IntStream.range(0, 3).forEach(x -> estECEF[x] = estECEF[x] + DeltaX.get(x, 0));
				approxRcvrClkDrift = DeltaX.get(3);
				error = Math.sqrt(IntStream.range(0, 3).mapToDouble(i -> Math.pow(DeltaX.get(i, 0), 2)).reduce(0,
						(i, j) -> i + j));
				System.out.print("");

			}
			setEstECEF(estECEF);
			setCovdX(HtWHinv);
			setApproxRcvrClkDrift(approxRcvrClkDrift / SpeedofLight);

			return;
		}
		System.out.println("Satellite count is less than 4, can't compute user position");

	}

}
