package com.RINEX_parser.ComputeUserPos.KalmanFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.stream.IntStream;

import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.jfree.ui.RefineryUtilities;
import org.orekit.models.earth.Geoid;

import com.RINEX_parser.ComputeUserPos.KalmanFilter.Models.StaticKF;
import com.RINEX_parser.ComputeUserPos.Regression.WLS;
import com.RINEX_parser.fileParser.IONEX;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.GraphPlotter;
import com.RINEX_parser.utility.SatUtil;

public class StaticEKF {

	private StaticKF kfObj;
	private ArrayList<ArrayList<Satellite>> SVlist;
	private double[] trueUserECEF;

	private static final double SpeedofLight = 299792458;
	private double obsNoiseVar;
	private ArrayList<Calendar> timeList;

	// For Tropo Corr PR, as Orekit Geoid requires LLH
	private double[] refLatLon;
	private SatUtil satUtil;
	private WLS wls;
	private double[] PCO;

	public StaticEKF(ArrayList<ArrayList<Satellite>> SVlist, double[] PCO, double[] trueUserECEF, IonoCoeff ionoCoeff,
			IONEX ionex, Geoid geoid, ArrayList<Calendar> timeList) {
		kfObj = new StaticKF();
		this.SVlist = SVlist;
		this.PCO = PCO;
		this.trueUserECEF = trueUserECEF;

		this.timeList = timeList;
		WLS wls = new WLS(SVlist.get(0), PCO, ionoCoeff, timeList.get(0), ionex, geoid);
		double[] refECEF = wls.getTropoCorrECEF();
		refLatLon = ECEFtoLatLon.ecef2lla(refECEF);
		satUtil = new SatUtil(PCO, ionoCoeff, null, ionex, geoid);
		System.out.println("True User ECEF - "
				+ Arrays.stream(trueUserECEF).mapToObj(i -> String.valueOf(i)).reduce("", (i, j) -> i + " " + j));
//		System.out.println("Reference User Position to calculate H jacobian - "
//				+ Arrays.stream(approxECEF).mapToObj(i -> String.valueOf(i)).reduce("", (i, j) -> i + " " + j));

//		double[][] x = new double[][] { { 1000 }, { 1000 }, { 1000 }, { 0.1 }, { 0.001 } };
		double[][] x = new double[][] { { refECEF[0] }, { refECEF[1] }, { refECEF[2] }, { 0.1 }, { 0.001 } };
		System.out.println("Intial State Estimate - " + IntStream.range(0, x.length)
				.mapToObj(i -> String.valueOf(x[i][0])).reduce("", (i, j) -> i + " " + j));

		double[][] P = new double[5][5];
		IntStream.range(0, 5).forEach(i -> P[i][i] = 16);
		P[3][3] = 1e13;
		P[4][4] = 1e13;
		System.out.println("Intial Estimate Error Covariance - " + IntStream.range(0, P.length)
				.mapToObj(i -> String.valueOf(P[i][i])).reduce("", (i, j) -> i + " " + j));
		obsNoiseVar = 9;
		kfObj.setState(x, P);
	}

	public ArrayList<double[]> compute() {

		ArrayList<double[]> ecefList = new ArrayList<double[]>();
		ArrayList<Double> errList = new ArrayList<Double>();
		ArrayList<Double> errCovList = new ArrayList<Double>();
		// In Seconds
		double time = timeList.get(0).getTimeInMillis() / 1E3;
		for (int i = 1; i < SVlist.size(); i++) {

			System.out.println("Step/Itr - " + i);
			ArrayList<Satellite> SV = SVlist.get(i);
			double currentTime = timeList.get(i).getTimeInMillis() / 1E3;
			double deltaT = (int) (currentTime - time);
			runFilter3(deltaT, SV, timeList.get(i));
			SimpleMatrix x = kfObj.getState();
			SimpleMatrix P = kfObj.getCovariance();
			double[] estECEF = new double[] { x.get(0), x.get(1), x.get(2) };
			ecefList.add(estECEF);
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
//		chartPack("Position Error", path + "_err.PNG", errList, timeList, 100);
//		chartPack("Error Covariance", path + "_cov.PNG", errCovList, timeList, 100);

		System.out.println(kfObj.getState().toString());
		return ecefList;
	}

	public void runFilter1(double deltaT, ArrayList<Satellite> SV, Calendar time) {

		int SVcount = SV.size();

		kfObj.configureSPP(deltaT);
		kfObj.predict();

		SimpleMatrix x = kfObj.getState();
		double[] estECEF = new double[] { x.get(0) + PCO[0], x.get(1) + PCO[1], x.get(2) + PCO[2], x.get(3) };
		double[][] unitLOS = SatUtil.getUnitLOS(SV, estECEF);
		// H is the Jacobian matrix of partial derivatives Observation Model(h) of with
		// respect to x
		double[][] H = new double[SVcount][5];
		IntStream.range(0, SVcount).forEach(i -> {
			IntStream.range(0, 3).forEach(j -> H[i][j] = -unitLOS[i][j]);
			H[i][3] = 1;
		});

		double[][] z = new double[SVcount][1];
		// get satellite clock offset error, Iono and Tropo errors corrected pseudorange
		double[] tropoCorrPR = satUtil.getTropoCorrPR(SV, time, refLatLon);

		IntStream.range(0, SVcount).forEach(i -> z[i][0] = tropoCorrPR[i]);
		double[][] ze = new double[SVcount][1];

		IntStream.range(0, SVcount).forEach(
				i -> ze[i][0] = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> estECEF[j] - SV.get(i).getECI()[j])
						.map(j -> j * j).reduce(0, (j, k) -> j + k)) + (estECEF[3]));
		double[][] R = new double[SVcount][SVcount];// Weight.computeCovMat(SV);
		IntStream.range(0, SVcount).forEach(i -> R[i][i] = obsNoiseVar);
		kfObj.update(z, R, ze, H);

	}

	public void runFilter2(double deltaT, ArrayList<Satellite> SV, double[] PCO, Calendar time) {
		double[][] H = new double[4][5];
		IntStream.range(0, 4).forEach(i -> H[i][i] = 1);
		H[3][4] = deltaT;
		kfObj.configureSPP(deltaT);
		kfObj.predict();

		double[] mECEF = wls.getIonoCorrECEF();
		double[][] z = new double[][] { { mECEF[0] }, { mECEF[1] }, { mECEF[2] }, { wls.getRcvrClkOff() } };
		double[][] ze = new double[4][1];
		SimpleMatrix x = kfObj.getState();
		IntStream.range(0, 3).forEach(i -> ze[i][0] = x.get(i));
		ze[3][0] = x.get(3) + (x.get(4) * deltaT);
		SimpleMatrix covdX = wls.getCovdX();
		double[][] R = new double[4][4];

		// IntStream.range(0, 4).forEach(i -> R[i][i] = covdX.get(i, i) * ObsNoiseVar);
//		double sum = covdX.elementSum();
//		IntStream.range(0, 4)
//				.forEach(i -> IntStream.range(0, 4).forEach(j -> R[i][j] = covdX.get(i, j) * ObsNoiseVar / sum));
		SimpleMatrix _R = new SimpleMatrix(R);
		System.out.println("Measurement Noise  " + _R.toString());
		if (!MatrixFeatures_DDRM.isPositiveDefinite(_R.getMatrix())) {
			System.out.println("PositiveDefinite test Failed for R");
		}

		kfObj.update(z, R, ze, H);

	}

	// Iterated EKF
	public void runFilter3(double deltaT, ArrayList<Satellite> SV, Calendar time) {

		int SVcount = SV.size();

		kfObj.configureSPP(deltaT);
		kfObj.predict();

		SimpleMatrix x = kfObj.getState();
		double[] estECEF = new double[] { x.get(0) + PCO[0], x.get(1) + PCO[1], x.get(2) + PCO[2], x.get(3) };

		double[][] z = new double[SVcount][1];
		// get satellite clock offset error, Iono and Tropo errors corrected pseudorange
		double[] tropoCorrPR = satUtil.getTropoCorrPR(SV, time, refLatLon);

		IntStream.range(0, SVcount).forEach(i -> z[i][0] = tropoCorrPR[i]);
		double[][] ze = new double[SVcount][1];

		IntStream.range(0, SVcount).forEach(
				i -> ze[i][0] = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> estECEF[j] - SV.get(i).getECI()[j])
						.map(j -> j * j).reduce(0, (j, k) -> j + k)) + (estECEF[3]));
		double[][] R = new double[SVcount][SVcount];// Weight.computeCovMat(SV);
		IntStream.range(0, SVcount).forEach(i -> R[i][i] = obsNoiseVar);
		double[][] satECI = new double[SVcount][];
		IntStream.range(0, SVcount).forEach(i -> satECI[i] = SV.get(i).getECI());
		kfObj.update(z, R, ze, satECI, PCO);

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
