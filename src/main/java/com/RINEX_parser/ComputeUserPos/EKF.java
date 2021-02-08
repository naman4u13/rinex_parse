package com.RINEX_parser.ComputeUserPos;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;
import org.jfree.ui.RefineryUtilities;

import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.GraphPlotter;
import com.RINEX_parser.utility.SatUtil;

//ref - https://link.springer.com/article/10.1007/s10291-015-0480-2
// ref - https://ieeexplore.ieee.org/document/6702097
// ref - Introduction to Random Signals and Applied Kalman Filtering
public class EKF {
	static double SpeedofLight = 299792458;

	public static void compute(ArrayList<ArrayList<Satellite>> SVlist, ArrayList<Calendar> timeList,
			double[] trueUserECEF) {

		SatUtil satUtil = new SatUtil(SVlist.get(0));
		double[] approxECEF = satUtil.getUserECEF();
		System.out.println(trueUserECEF);
		ArrayList<Double> errList = new ArrayList<Double>();
		// Intial State Vector = [ECEF_X, ECEF_Y, ECEF_Z, Rcvr Clk Offset, Rcvr Clk
		// Drift ]
//		SimpleMatrix postStateEst = new SimpleMatrix(
//				new double[][] { { 1000 }, { 1000 }, { 1000 }, { 0.1 }, { 0.001 } });
//		
		SimpleMatrix postStateEst = new SimpleMatrix(new double[][] { { approxECEF[0] }, { approxECEF[1] },
				{ approxECEF[2] }, { approxECEF[3] }, { 0.001 } });

		double[][] _postEstErr = new double[5][5];
		IntStream.range(0, 5).forEach(i -> _postEstErr[i][i] = 100);
		SimpleMatrix postEstErr = new SimpleMatrix(_postEstErr);

		// Typical Allan Variance Coefficients for TCXO (low quality)
		double h0 = 2E-19;
		double h_2 = 2E-20;
		double sf = h0 / 2;
		double sg = 2 * Math.PI * Math.PI * h_2;

		// In Seconds
		double time = timeList.get(0).getTimeInMillis() / 1E3;

		for (int i = 0; i < SVlist.size(); i++) {

			ArrayList<Satellite> SV = SVlist.get(i);
			int SVcount = SV.size();
			double currentTime = timeList.get(i).getTimeInMillis() / 1E3;
			double deltaT = (int) (currentTime - time);
			double[][] _stateTrans = new double[5][5];
			IntStream.range(0, 5).forEach(x -> _stateTrans[x][x] = 1);
			_stateTrans[3][4] = deltaT;
			SimpleMatrix stateTrans = new SimpleMatrix(_stateTrans);
			double[][] _processNoise = new double[5][5];
			double c2 = SpeedofLight * SpeedofLight;

			_processNoise[3][3] = (sf * deltaT) + ((sg * Math.pow(deltaT, 3)) / 3);
			_processNoise[3][4] = (sg * Math.pow(deltaT, 2)) / 2;
			_processNoise[4][3] = (sg * Math.pow(deltaT, 2)) / 2;
			_processNoise[4][4] = sg * deltaT;
			SimpleMatrix processNoise = new SimpleMatrix(_processNoise).scale(c2);

			// Prediction Step / Time Update

			// calculating K-th priorStateEst using (K-1)-th postStateEst
			SimpleMatrix priorStateEst = stateTrans.mult(postStateEst);
			SimpleMatrix priorEstErr = (stateTrans.mult(postEstErr).mult((stateTrans.transpose()))).plus(processNoise);

			// Let std. deviation or sigma for PR error be 10 meter
			// ref
			// -(https://gnss-compare.readthedocs.io/en/latest/user_manual/implemented_algorithms.html)
			double[][] cov_dp = new double[SVcount][SVcount];
			IntStream.range(0, SVcount).forEach(x -> cov_dp[x][x] = 25);
			SimpleMatrix measurementNoise = new SimpleMatrix(cov_dp);

			double[][] unitLOS = satUtil.getUnitLOS(SV);
			// H is the Jacobian matrix of partial derivatives Observation Model(h) of with
			// respect to x
			double[][] _H = new double[SVcount][5];
			IntStream.range(0, SVcount).forEach(x -> {
				IntStream.range(0, 3).forEach(y -> _H[x][y] = -unitLOS[x][y]);
				_H[x][3] = 1;
			});

			SimpleMatrix H = new SimpleMatrix(_H);

			double[][] _measurement = new double[SVcount][1];
			// Removed satellite clock offset error from pseudorange
			IntStream.range(0, SVcount).forEach(
					x -> _measurement[x][0] = SV.get(x).getPseudorange() + (SpeedofLight * SV.get(x).getSatClkOff()));

			SimpleMatrix measurement = new SimpleMatrix(_measurement);

			double[][] _measurementEst = new double[SVcount][1];
			double[] estUserECEF = new double[] { priorStateEst.get(0), priorStateEst.get(1), priorStateEst.get(2),
					priorStateEst.get(3) };
			IntStream.range(0, SVcount)
					.forEach(x -> _measurementEst[x][0] = Math
							.sqrt(IntStream.range(0, 3).mapToDouble(y -> estUserECEF[y] - SV.get(x).getECEF()[y])
									.map(y -> y * y).reduce(0, (y, z) -> y + z))
							+ (estUserECEF[3]));
			SimpleMatrix measurementEst = new SimpleMatrix(_measurementEst);

			// update step
//			SimpleMatrix[] updatedVals = update(priorEstErr, H, measurementNoise, priorStateEst, measurement,
//					measurementEst);
			SimpleMatrix[] updatedVals = update2(priorEstErr, _H, cov_dp, priorStateEst, _measurement, SV);
			postStateEst = updatedVals[0];
			postEstErr = updatedVals[1];

			time = currentTime;
			double[] estECEF = new double[] { postStateEst.get(0), postStateEst.get(1), postStateEst.get(2) };
			double err = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> estECEF[x] - trueUserECEF[x]).map(x -> x * x)
					.reduce(0, (x, y) -> x + y));

			System.out.println(i);

			// System.out.println("postStateEst -" + postStateEst.toString());
			// System.out.println("priorEstErr -" + priorEstErr.toString());
			// System.out.println("postEstErr -" + postEstErr.toString());
			System.out.println(err);
			errList.add(err);
		}
		HashMap<String, ArrayList<Double>> errMap = new HashMap<String, ArrayList<Double>>();
		errMap.put("EKF error", errList);
		GraphPlotter chart = new GraphPlotter("EKF - ", "EKF", timeList, errMap);

		chart.pack();
		RefineryUtilities.positionFrameRandomly(chart);
		chart.setVisible(true);
		System.out.println(postStateEst.toString());
	}

	// update
	public static SimpleMatrix[] update(SimpleMatrix Pminus, SimpleMatrix H, SimpleMatrix R, SimpleMatrix Xminus,
			SimpleMatrix Z, SimpleMatrix Zest) {
		SimpleMatrix Ht = H.transpose();
		// Kalman Gain
		SimpleMatrix K = Pminus.mult(Ht).mult(((H.mult(Pminus).mult(Ht)).plus(R)).invert());

		SimpleMatrix temp = (H.mult(Pminus).mult(Ht));
		// System.out.println("HPHt -" + temp.toString());
		// System.out.println("Kalman -" + K.toString());
		// Posterior State Estimate
		SimpleMatrix Xplus = Xminus.plus((K.mult(Z.minus(Zest))));
		SimpleMatrix KH = K.mult(H);
		SimpleMatrix I = SimpleMatrix.identity(KH.numRows());
		// Posterior Estimate Error
		SimpleMatrix Pplus = (I.minus(KH)).mult(Pminus);
		SimpleMatrix Pplus2 = ((I.minus(KH)).mult(Pminus).mult((I.minus(KH)).transpose()))
				.plus(K.mult(R).mult(K.transpose()));
		System.out.println("PostEstErr -" + Pplus.toString());
		System.out.println("PostEstErr2 -" + Pplus2.toString());
		return new SimpleMatrix[] { Xplus, Pplus2 };

	}

	// Sequential Update
	public static SimpleMatrix[] update2(SimpleMatrix _Pminus, double[][] _H, double[][] _R, SimpleMatrix _Xminus,
			double[][] _Z, ArrayList<Satellite> SV) {
		SimpleMatrix P = new SimpleMatrix(_Pminus);
		SimpleMatrix X = new SimpleMatrix(_Xminus);
		int SVcount = SV.size();
		for (int i = 0; i < SVcount; i++) {
			SimpleMatrix H = new SimpleMatrix(new double[][] { _H[i] });
			SimpleMatrix R = new SimpleMatrix(new double[][] { { _R[i][i] } });
			SimpleMatrix denom = H.mult(P).mult(H.transpose()).plus(R);
			SimpleMatrix K = P.mult(H.transpose()).scale(1 / denom.get(0));
			SimpleMatrix KH = K.mult(H);
			SimpleMatrix I = SimpleMatrix.identity(KH.numRows());
			// P = P.minus(K.mult(H).mult(P));
			P = ((I.minus(KH)).mult(P).mult((I.minus(KH)).transpose())).plus(K.mult(R).mult(K.transpose()));

			SimpleMatrix Z = new SimpleMatrix(new double[][] { _Z[i] });

			double[][] _Zest = new double[1][1];
			double[] estUserECEF = new double[] { X.get(0), X.get(1), X.get(2), X.get(3) };
			double[] SVecef = SV.get(i).getECEF();
			_Zest[0][0] = Math.sqrt(IntStream.range(0, 3).mapToDouble(y -> estUserECEF[y] - SVecef[y]).map(y -> y * y)
					.reduce(0, (y, z) -> y + z)) + (estUserECEF[3]);
			SimpleMatrix Zest = new SimpleMatrix(_Zest);

			X = X.plus((K.mult(Z.minus(Zest))));
		}
		return new SimpleMatrix[] { X, P };

	}

	public static void compute2(ArrayList<ArrayList<Satellite>> SVlist, ArrayList<Calendar> timeList) {

		// Intial State Vector = [ECEF_X, ECEF_Y, ECEF_Z, Rcvr Clk Offset, Rcvr Clk
		// Drift ]
		SimpleMatrix postStateEst = new SimpleMatrix(new double[][] { { 1000, 1000, 1000, 0.1, 0.001 } });

		double[][] _postEstErr = new double[5][5];
		IntStream.range(0, 5).forEach(i -> _postEstErr[i][i] = 10);
		SimpleMatrix postEstErr = new SimpleMatrix(_postEstErr);

		// Typical Allan Variance Coefficients for TCXO (low quality)
		double h0 = 2E-19;
		double h_2 = 2E-20;
		double sf = h0 / 2;
		double sg = 2 * Math.PI * Math.PI * h_2;

		// In Seconds
		double time = timeList.get(0).getTimeInMillis() / 1E3;

		SatUtil satUtil = new SatUtil(SVlist.get(0));

		for (int i = 0; i < SVlist.size(); i++) {

			ArrayList<Satellite> SV = SVlist.get(i);
			int SVcount = SV.size();
			double currentTime = timeList.get(i).getTimeInMillis() / 1E3;
			double deltaT = currentTime - time;
			double[][] _stateTrans = new double[5][5];
			IntStream.range(0, 5).forEach(x -> _stateTrans[x][x] = 1);
			_stateTrans[3][4] = deltaT;
			SimpleMatrix stateTrans = new SimpleMatrix(_stateTrans);
			double[][] _processNoise = new double[5][5];
			_processNoise[3][3] = (sf * deltaT) + ((sg * Math.pow(deltaT, 3)) / 3);
			_processNoise[3][4] = (sg * Math.pow(deltaT, 2)) / 2;
			_processNoise[4][3] = (sg * Math.pow(deltaT, 2)) / 2;
			_processNoise[4][4] = sg * deltaT;
			SimpleMatrix processNoise = new SimpleMatrix(_processNoise);

			// Prediction Step / Time Update

			// calculating K-th priosStateEst using (K-1)-th postStateEst
			SimpleMatrix priorStateEst = stateTrans.mult(postStateEst);
			SimpleMatrix priorEstErr = (stateTrans.mult(postEstErr).mult((stateTrans.transpose()))).plus(processNoise);

			double[][] weightMat = satUtil.getWeightMat(SV);
			SimpleMatrix cov_dp = new SimpleMatrix(weightMat);
			double[][] unitLOS = satUtil.getUnitLOS(SV);
			SimpleMatrix H = new SimpleMatrix(unitLOS);
			SimpleMatrix K = (((H.transpose()).mult(H)).invert()).mult(H.transpose());
			// Understanding GPS Principles and Applications Second Edition Elliott D.
			// Kaplan Christopher J. Hegarty
			// page - 326
			// ECEF pos and rcvr clock offset error or covariance matrix - [dx,dy,dz,c*dt_u]
			SimpleMatrix cov_dx = K.mult(cov_dp).mult((K.transpose()));
			// connection matrix for pseudorange rate for static case
			double[][] _H = new double[SVcount][1];
			IntStream.range(0, SVcount).forEach(x -> _H[x][0] = 1);
			H = new SimpleMatrix(_H);
			K = (((H.transpose()).mult(H)).invert()).mult(H.transpose());
			// SimpleMatrix cov_dp_dot =
			double[][] _meaurementNoise = new double[1][1];

			time = currentTime;
		}
	}

}
