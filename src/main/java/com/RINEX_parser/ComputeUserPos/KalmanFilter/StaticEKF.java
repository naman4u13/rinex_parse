package com.RINEX_parser.ComputeUserPos.KalmanFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.stream.IntStream;

import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.jfree.ui.RefineryUtilities;

import com.RINEX_parser.ComputeUserPos.KalmanFilter.Models.StaticKF;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.GraphPlotter;
import com.RINEX_parser.utility.SatUtil;

public class StaticEKF {

	private SatUtil satUtil;

	private StaticKF kfObj = new StaticKF();
	private ArrayList<ArrayList<Satellite>> SVlist;
	private double[] trueUserECEF;
	private IonoCoeff ionoCoeff;
	private static final double SpeedofLight = 299792458;
	private int ObsNoiseVar;

	public StaticEKF(ArrayList<ArrayList<Satellite>> SVlist, double[] trueUserECEF, IonoCoeff ionoCoeff) {
		this.SVlist = SVlist;
		this.trueUserECEF = trueUserECEF;
		this.ionoCoeff = ionoCoeff;
		satUtil = new SatUtil(SVlist.get(0));
		double[] approxECEF = satUtil.getUserECEF();
		System.out.println("True User ECEF - "
				+ Arrays.stream(trueUserECEF).mapToObj(i -> String.valueOf(i)).reduce("", (i, j) -> i + " " + j));
		System.out.println("Reference User Position to calculate Z_hat - "
				+ Arrays.stream(approxECEF).mapToObj(i -> String.valueOf(i)).reduce("", (i, j) -> i + " " + j));

		double[][] x = new double[][] { { 1000 }, { 1000 }, { 1000 }, { 0.1 }, { 0.001 } };
		System.out.println("Intial State Estimate - " + IntStream.range(0, x.length)
				.mapToObj(i -> String.valueOf(x[i][0])).reduce("", (i, j) -> i + " " + j));

		double[][] P = new double[5][5];
		IntStream.range(0, 5).forEach(i -> P[i][i] = 1e13);
		System.out.println("Intial Estimate Error Covariance - " + IntStream.range(0, P.length)
				.mapToObj(i -> String.valueOf(P[i][i])).reduce("", (i, j) -> i + " " + j));

		ObsNoiseVar = 10;
		System.out.println("ObsNoiseVar(R) - " + ObsNoiseVar);
		kfObj.setState(x, P);
	}

	public void compute(ArrayList<Calendar> timeList, String path) {

		ArrayList<Double> errList = new ArrayList<Double>();
		ArrayList<Double> errCovList = new ArrayList<Double>();
		// In Seconds
		double time = timeList.get(0).getTimeInMillis() / 1E3;
		for (int i = 0; i < SVlist.size(); i++) {

			System.out.println("Step/Itr - " + i);
			ArrayList<Satellite> SV = SVlist.get(i);
			double currentTime = timeList.get(i).getTimeInMillis() / 1E3;
			double deltaT = (int) (currentTime - time);
			runFilter1(deltaT, SV);
			SimpleMatrix x = kfObj.getState();
			SimpleMatrix P = kfObj.getCovariance();
			double[] estECEF = new double[] { x.get(0), x.get(1), x.get(2) };
			double err = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> estECEF[j] - trueUserECEF[j]).map(j -> j * j)
					.reduce(0, (j, k) -> j + k));
			System.out.println("postEstErr -" + P.toString());
			if (!MatrixFeatures_DDRM.isPositiveDefinite(P.getMatrix())) {
				System.out.println("PositiveDefinite test Failed");
			}
			System.out.println("Pos Error - " + err);

			double TraceOfP = P.trace();
			System.out.println(" Trace of ErrEstCov - " + TraceOfP);
			errList.add(err);
			errCovList.add(TraceOfP);
			time = currentTime;

		}
		chartPack("Position Error", path + "_err.PNG", errList, timeList, 100);
		chartPack("Error Covariance", path + "_cov.PNG", errCovList, timeList, 10);

		System.out.println(kfObj.getState().toString());

	}

	public void runFilter1(double deltaT, ArrayList<Satellite> SV) {

		int SVcount = SV.size();
		double[][] unitLOS = satUtil.getUnitLOS(SV);
		kfObj.predict(deltaT, unitLOS);

		double[][] z = new double[SVcount][1];
		// Compute Iono corrections
		double[] ionoCorrPR = satUtil.getIonoCorrPR(SV, ionoCoeff);
		// Removed satellite clock offset error and Iono errors from pseudorange
		IntStream.range(0, SVcount).forEach(i -> z[i][0] = ionoCorrPR[i]);
		double[][] ze = new double[SVcount][1];
		SimpleMatrix x = kfObj.getState();
		double[] estUserECEF = new double[] { x.get(0), x.get(1), x.get(2), x.get(3) };
		IntStream.range(0, SVcount)
				.forEach(i -> ze[i][0] = Math
						.sqrt(IntStream.range(0, 3).mapToDouble(j -> estUserECEF[j] - SV.get(i).getECEF()[j])
								.map(j -> j * j).reduce(0, (j, k) -> j + k))
						+ (estUserECEF[3]));
		double[][] R = new double[SVcount][SVcount];
		IntStream.range(0, SVcount).forEach(i -> R[i][i] = ObsNoiseVar);
		kfObj.update(z, R, ze);

	}

	public void runFilter2(double deltaT, ArrayList<Satellite> SV) {
		int SVcount = SV.size();
		kfObj.predict(deltaT);

	}

	public static void chartPack(String title, String path, ArrayList<Double> dataList, ArrayList<Calendar> timeList,
			long max) {
		HashMap<String, ArrayList<Double>> errMap = new HashMap<String, ArrayList<Double>>();
		errMap.put(title, dataList);
		GraphPlotter chart = new GraphPlotter(title, title, timeList, errMap, max, path);
		chart.pack();
		RefineryUtilities.positionFrameRandomly(chart);
		chart.setVisible(true);
	}
}
