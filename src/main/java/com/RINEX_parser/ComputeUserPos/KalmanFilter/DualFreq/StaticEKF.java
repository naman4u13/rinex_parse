package com.RINEX_parser.ComputeUserPos.KalmanFilter.DualFreq;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.stream.IntStream;

import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.jfree.ui.RefineryUtilities;
import org.orekit.models.earth.Geoid;

import com.RINEX_parser.ComputeUserPos.KalmanFilter.Models.StaticKF;
import com.RINEX_parser.ComputeUserPos.Regression.WLS;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.GraphPlotter;
import com.RINEX_parser.utility.SatUtil;

public class StaticEKF {

	private StaticKF kfObj;
	private ArrayList<ArrayList<Satellite>[]> dualSVlist;
	private double[] trueUserECEF;

	private static final double SpeedofLight = 299792458;
	private double obsNoiseVar;
	private ArrayList<Calendar> timeList;

	// For Tropo Corr PR, as Orekit Geoid requires LLH
	private double[] refECEF;
	private SatUtil satUtil;
	private WLS wls;
	private double[] PCO;
	private double[] fSq;

	public StaticEKF(ArrayList<ArrayList<Satellite>[]> dualSVlist, double[][] PCO, double[] refECEF,
			double[] trueUserECEF, Geoid geoid, ArrayList<Calendar> timeList) {
		kfObj = new StaticKF();
		this.dualSVlist = dualSVlist;
		fSq = new double[2];
		fSq[0] = Math.pow(dualSVlist.get(0)[0].get(0).getCarrier_frequency(), 2);
		fSq[1] = Math.pow(dualSVlist.get(0)[1].get(0).getCarrier_frequency(), 2);
		this.trueUserECEF = trueUserECEF;

		this.timeList = timeList;
		this.refECEF = refECEF;
		satUtil = new SatUtil(PCO, geoid);

		this.PCO = getIonoFree(PCO);

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

		for (int i = 1; i < dualSVlist.size(); i++) {

			System.out.println("Step/Itr - " + i);
			ArrayList<Satellite>[] SV = dualSVlist.get(i);
			double currentTime = timeList.get(i).getTimeInMillis() / 1E3;
			double deltaT = (int) (currentTime - time);
			runFilter1(deltaT, SV, timeList.get(i));
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
//		

		System.out.println(kfObj.getState().toString());
		return ecefList;
	}

	// Iterated EKF
	public void runFilter1(double deltaT, ArrayList<Satellite>[] SV, Calendar time) {

		int SVcount = SV[0].size();

		kfObj.configure(deltaT);
		kfObj.predict();

		SimpleMatrix x = kfObj.getState();
		double[] estECEF = new double[] { x.get(0) + PCO[0], x.get(1) + PCO[1], x.get(2) + PCO[2], x.get(3) };

		double[][] z = new double[SVcount][1];
		// get satellite clock offset error, Tropo errors corrected pseudorange
		double[][] tropoCorrPR = satUtil.getTropoCorrPR(SV, time, refECEF);

		double[][] satECI = new double[SVcount][3];
		double[] IFtropoCorrPR = new double[SVcount];

		for (int i = 0; i < SVcount; i++) {
			satECI[i] = getIonoFree(SV[0].get(i).getECI(), SV[1].get(i).getECI());
			IFtropoCorrPR[i] = getIonoFree(tropoCorrPR[0][i], tropoCorrPR[1][i]);
		}

		IntStream.range(0, SVcount).forEach(i -> z[i][0] = IFtropoCorrPR[i]);
		double[][] ze = new double[SVcount][1];

		IntStream.range(0, SVcount)
				.forEach(i -> ze[i][0] = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> estECEF[j] - satECI[i][j])
						.map(j -> j * j).reduce(0, (j, k) -> j + k)) + (estECEF[3]));
		double[][] R = new double[SVcount][SVcount];// Weight.computeCovMat(SV);
		IntStream.range(0, SVcount).forEach(i -> R[i][i] = obsNoiseVar);
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

	private double[] getIonoFree(double[][] value) {
		return getIonoFree(value[0], value[1]);
	}

	private double[] getIonoFree(double[] val1, double[] val2) {
		double[] ionoFreeValue = new double[3];
		int n = val1.length;
		for (int i = 0; i < n; i++) {
			ionoFreeValue[i] = getIonoFree(val1[i], val2[i]);
		}
		return ionoFreeValue;
	}

	private double getIonoFree(double val1, double val2) {
		return ((fSq[0] * val1) - (fSq[1] * val2)) / (fSq[0] - fSq[1]);
	}

}
