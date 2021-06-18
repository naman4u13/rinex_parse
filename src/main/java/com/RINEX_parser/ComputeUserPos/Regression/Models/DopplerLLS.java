package com.RINEX_parser.ComputeUserPos.Regression.Models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;

import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;

public class DopplerLLS extends LinearLeastSquare {

	private final static double SpeedofLight = 299792458;

	public DopplerLLS(ArrayList<Satellite> SV, double[] PCO, IonoCoeff ionoCoeff, Calendar time) {
		super(SV, PCO, ionoCoeff, time, null, null, null);
		// TODO Auto-generated constructor stub
	}

	public void estimate(double[] PR, boolean isStatic) {
		estimate(PR, getWeight(), isStatic);
	}

	// reference - DopplerLLS-Aided GNSS Position Estimation With Weighted Least
	// Squares
	// https://ieeexplore.ieee.org/document/5976479/
	// reference - A-GPS: Assisted GPS, GNSS, and SBASprint: Frank van Diggelen,
	// chapter
	// 8

	public void estimate(double[] PR, double[][] Weight, boolean isStatic) {
		double[] estECEF = new double[] { 0, 0, 0 };
		SimpleMatrix HtWHinv = null;
		double approxRcvrClkOff = 0;
		double approxRcvrClkDrift = 0;
		double[] estVel = new double[] { 0, 0, 0 };
		ArrayList<Satellite> SV = getSV();
		int SVcount = SV.size();

		double error = Double.MAX_VALUE;
		// Get Millimeter Accuracy, actually it takes atleast 5 iterations to converge
		// to the result which is accurate more than micrometer scale
		double threshold = 1e-3;

		if (SVcount >= 4) {

			while (error >= threshold) {
				double[][] deltaPR = new double[SVcount][1];
				double[][] deltaPRrate = new double[SVcount][1];

				double[][] Hpp = new double[SVcount][5];
				double[][] Hfp = new double[SVcount][5];
				double[][] Hfv = new double[SVcount][3];
				for (int j = 0; j < SVcount; j++) {

					Satellite sat = SV.get(j);
					double[] satECEF = sat.getECEF();

					double approxGR = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> satECEF[x] - estECEF[x])
							.map(x -> Math.pow(x, 2)).reduce((x, y) -> x + y).getAsDouble());

					double approxPR = approxGR + (SpeedofLight * approxRcvrClkOff);
					deltaPR[j][0] = PR[j] - approxPR;
					double[] satVel = sat.getSatVel();
					double[] unitLOS = IntStream.range(0, 3).mapToDouble(k -> (satECEF[k] - estECEF[k]) / approxGR)
							.toArray();
					double approxGRrate = IntStream.range(0, 3).mapToDouble(k -> (satVel[k] - estVel[k]) * unitLOS[k])
							.reduce(0, (k, l) -> k + l);
					double approxPRrate = approxGRrate + (SpeedofLight * approxRcvrClkDrift);
					// Corrected PRrate value, note we have ignored atmospheric
					// error drift
					double measPRrate = sat.getPseudoRangeRate() + (SpeedofLight * sat.getSatClkDrift());

					deltaPRrate[j][0] = measPRrate - approxPRrate;

					for (int k = 0; k < 3; k++) {
						Hpp[j][k] = -unitLOS[k];
						Hfv[j][k] = Hpp[j][k];
						Hfp[j][k] = (satVel[k] - (approxGRrate * unitLOS[k])) / approxGR;
					}
					Hpp[j][3] = 1;
					Hpp[j][4] = 0;
					Hfp[j][3] = 0;
					Hfp[j][4] = 1;

				}
				SimpleMatrix H;
				if (isStatic) {
					// H = [{Hpp,0},{Hfp}]
					H = (new SimpleMatrix(Hpp)).concatRows((new SimpleMatrix(Hfp)));
				} else {
					// H = [{Hpp,0},{Hfp,Hfv}]
					H = (new SimpleMatrix(Hpp))
							.concatRows((new SimpleMatrix(Hfp)).concatColumns(new SimpleMatrix(Hfv)));
				}
				SimpleMatrix Ht = H.transpose();
				SimpleMatrix W = new SimpleMatrix(Weight);
				HtWHinv = (Ht.mult(W).mult(H)).invert();
				SimpleMatrix DeltaY = new SimpleMatrix(deltaPR).concatRows(new SimpleMatrix(deltaPRrate));
				SimpleMatrix DeltaX = HtWHinv.mult(Ht).mult(W).mult(DeltaY);
				IntStream.range(0, 3).forEach(x -> estECEF[x] = estECEF[x] + DeltaX.get(x, 0));
				approxRcvrClkOff += DeltaX.get(3, 0) / SpeedofLight;
				approxRcvrClkDrift += DeltaX.get(4, 0) / SpeedofLight;
				// For Dynamic rcvr update the estVel
				if (!isStatic) {
					IntStream.range(5, 8).forEach(x -> estVel[x - 5] = estVel[x - 5] + DeltaX.get(x, 0));

				}
				error = Math.sqrt(IntStream.range(0, 3).mapToDouble(i -> Math.pow(DeltaX.get(i, 0), 2)).reduce(0,
						(i, j) -> i + j));

			}
			setEstECEF(estECEF);
			setEstVel(Arrays.stream(estVel).reduce(0, (i, j) -> i + j));
			setApproxRcvrClkOff(approxRcvrClkOff);
			setApproxRcvrClkDrift(approxRcvrClkDrift);
			setCovdX(HtWHinv);

			return;
		}
		System.out.println("Satellite count is less than 4, can't compute user position");

	}

}
