package com.RINEX_parser.ComputeUserPos.KalmanFilter.Models;

import java.util.ArrayList;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;

import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.SatUtil;

// Iterated Kalman Filter
public class StaticIKF extends StaticKF {

	public void update(double[][] _z, double[][] _R, double[][] _ze, ArrayList<Satellite> SV) {
		SimpleMatrix z = new SimpleMatrix(_z);
		SimpleMatrix R = new SimpleMatrix(_R);
		SimpleMatrix ze = new SimpleMatrix(_ze);
		SimpleMatrix xPrior = this.getState();
		SimpleMatrix P = this.getCovariance();
		SimpleMatrix H = null;
		SimpleMatrix K = null;
		SimpleMatrix x = null;
		int SVcount = SV.size();
		for (int i = 0; i < 5; i++) {
			x = xPrior;
			double[] estECEF = new double[] { x.get(0), x.get(1), x.get(2), x.get(3) };
			double[][] unitLOS = SatUtil.getUnitLOS(SV, estECEF);
			// H is the Jacobian matrix of partial derivatives Observation Model(h) of with
			// respect to x
			double[][] _H = new double[SVcount][5];
			IntStream.range(0, SVcount).forEach(j -> {
				IntStream.range(0, 3).forEach(k -> _H[j][k] = -unitLOS[j][k]);
				_H[j][3] = 1;
			});
			H = new SimpleMatrix(_H);
			SimpleMatrix Ht = H.transpose();
			// Kalman Gain

			K = P.mult(Ht).mult(((H.mult(P).mult(Ht)).plus(R)).invert());

			// Posterior State Estimate
			x = xPrior.plus((K.mult(z.minus(ze))));

		}
		SimpleMatrix KH = K.mult(H);
		SimpleMatrix I = SimpleMatrix.identity(KH.numRows());
		// Posterior Estimate Error
		// Joseph Form to ensure Positive Definiteness
		// P = (I-KH)P(I-KH)' + KRK'
		P = ((I.minus(KH)).mult(P).mult((I.minus(KH)).transpose())).plus(K.mult(R).mult(K.transpose()));
		setState(x, P);

	}

}
