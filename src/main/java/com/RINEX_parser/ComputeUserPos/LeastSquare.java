package com.RINEX_parser.ComputeUserPos;

import java.util.ArrayList;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;

import com.RINEX_parser.models.Satellite;

public class LeastSquare {

	public static double[] compute(ArrayList<Satellite> SV) {
		double SpeedofLight = 299792458;
		// Removed satellite clock offset error from pseudorange
		double[] PR = SV.stream().mapToDouble(x -> x.getPseudorange() + (SpeedofLight * x.getSatClkOff())).toArray();
		int SVcount = SV.size();
		if (SVcount >= 4) {
			double[] approxECEF = new double[] { 0, 0, 0 };
			double approxUserClkOff = 0;
			SimpleMatrix HtHinv = null;
			for (int i = 0; i < 100; i++) {
				double[][] deltaPR = new double[SVcount][1];
				// approx Geometric range or true range
				double[] approxGR = new double[SVcount];
				double[][] coeffA = new double[SVcount][4];
				for (int j = 0; j < SVcount; j++) {

					double[] satECEF = SV.get(j).getECEF();

					double ApproxGR = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> satECEF[x] - approxECEF[x])
							.map(x -> Math.pow(x, 2)).reduce((x, y) -> x + y).getAsDouble());
					approxGR[j] = ApproxGR;

					double approxPR = approxGR[j] + (SpeedofLight * approxUserClkOff);
					deltaPR[j][0] = approxPR - PR[j];
					int index = j;
					IntStream.range(0, 3).forEach(x -> coeffA[index][x] = (satECEF[x] - approxECEF[x]) / ApproxGR);
					coeffA[j][3] = 1;
				}
				SimpleMatrix H = new SimpleMatrix(coeffA);
				SimpleMatrix Ht = H.transpose();
				HtHinv = (Ht.mult(H)).invert();
				SimpleMatrix DeltaPR = new SimpleMatrix(deltaPR);
				SimpleMatrix DeltaX = HtHinv.mult(Ht).mult(DeltaPR);
				IntStream.range(0, 3).forEach(x -> approxECEF[x] = approxECEF[x] + DeltaX.get(x, 0));
				approxUserClkOff += (-DeltaX.get(3, 0)) / SpeedofLight;

			}
			if (HtHinv != null) {
				SimpleMatrix _HtHinv = HtHinv;
				double GDOP = Math
						.sqrt(IntStream.range(0, 3).mapToDouble(x -> _HtHinv.get(x, x)).reduce(0, (a, b) -> a + b));
				System.out.println(GDOP);
			}

			return approxECEF;

		}
		System.out.println("Satellite count is less than 4, can't compute user position");
		return new double[] { -999 };

	}

}
