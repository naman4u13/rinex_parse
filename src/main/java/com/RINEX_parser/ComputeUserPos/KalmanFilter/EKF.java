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

import com.RINEX_parser.ComputeUserPos.KalmanFilter.Models.KFconfig;
import com.RINEX_parser.ComputeUserPos.Regression.WLS;
import com.RINEX_parser.fileParser.IONEX;
import com.RINEX_parser.helper.ComputeEleAzm;
import com.RINEX_parser.helper.ComputeTropoCorr;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.GraphPlotter;
import com.RINEX_parser.utility.LatLonUtil;
import com.RINEX_parser.utility.SatUtil;

public class EKF {

	private KFconfig kfObj;
	private ArrayList<ArrayList<Satellite>> SVlist;
	private double[] trueUserECEF;

	private static final double SpeedofLight = 299792458;
	private double prObsNoiseVar;
	private ArrayList<Calendar> timeList;
	private double cpObsNoiseVar;
	// For Tropo Corr PR, as Orekit Geoid requires LLH
	private double[] refLatLon;
	private SatUtil satUtil;
	private WLS wls;
	private double[] PCO;
	private IonoCoeff ionoCoeff;
	private IONEX ionex;
	private Geoid geoid;

	public EKF(ArrayList<ArrayList<Satellite>> SVlist, double[] PCO, double[] trueUserECEF, IonoCoeff ionoCoeff,
			IONEX ionex, Geoid geoid, ArrayList<Calendar> timeList) {
		kfObj = new KFconfig();
		this.SVlist = SVlist;
		this.PCO = PCO;
		this.trueUserECEF = trueUserECEF;
		this.timeList = timeList;
		this.ionoCoeff = ionoCoeff;
		this.ionex = ionex;
		this.geoid = geoid;
		satUtil = new SatUtil(PCO, ionoCoeff, null, ionex, geoid);

	}

	public ArrayList<double[]> computeStatic() {
		WLS wls = new WLS(SVlist.get(0), PCO, ionoCoeff, timeList.get(0), ionex, geoid);
		double[] refECEF = wls.getTropoCorrECEF();
		refLatLon = ECEFtoLatLon.ecef2lla(refECEF);

		System.out.println("True User ECEF - "
				+ Arrays.stream(trueUserECEF).mapToObj(i -> String.valueOf(i)).reduce("", (i, j) -> i + " " + j));

		double[][] x = new double[][] { { refECEF[0] }, { refECEF[1] }, { refECEF[2] }, { 0.1 }, { 0.001 } };
		System.out.println("Intial State Estimate - " + IntStream.range(0, x.length)
				.mapToObj(i -> String.valueOf(x[i][0])).reduce("", (i, j) -> i + " " + j));

		double[][] P = new double[5][5];
		IntStream.range(0, 3).forEach(i -> P[i][i] = 16);
		P[3][3] = 1e13;
		P[4][4] = 1e13;
		System.out.println("Intial Estimate Error Covariance - " + IntStream.range(0, P.length)
				.mapToObj(i -> String.valueOf(P[i][i])).reduce("", (i, j) -> i + " " + j));
		prObsNoiseVar = 9;
		kfObj.setState(x, P);
		return iterateSPP(true, null);

	}

	public ArrayList<double[]> computeDynamicSPP(ArrayList<double[]> trueLLHlist) {
		WLS wls = new WLS(SVlist.get(0), PCO, ionoCoeff, timeList.get(0), ionex, geoid);
		double[] refECEF = wls.getTropoCorrECEF();
		double[][] x = new double[][] { { refECEF[0] }, { refECEF[1] }, { refECEF[2] }, { 0.1 } };
		double[][] P = new double[4][4];
		IntStream.range(0, 3).forEach(i -> P[i][i] = 100);
		P[3][3] = 9e10;
		prObsNoiseVar = 49;

		kfObj.setState(x, P);
		return iterateSPP(false, trueLLHlist);

	}

	public ArrayList<double[]> computeDynamicPPP(ArrayList<double[]> trueLLHlist, double[] intialECEF) {

		double[][] x = new double[][] { { intialECEF[0] }, { intialECEF[1] }, { intialECEF[2] }, { 0 }, { 0 } };
		double[][] P = new double[5][5];
		IntStream.range(0, 3).forEach(i -> P[i][i] = 1000);
		P[3][3] = 9e10;
		P[4][4] = 0.25;
		prObsNoiseVar = 1000;
		cpObsNoiseVar = 0.1;
		kfObj.setState(x, P);
		return iteratePPP(false, trueLLHlist);

	}

	public ArrayList<double[]> iteratePPP(boolean isStatic, ArrayList<double[]> trueLLHlist) {
		ArrayList<double[]> ecefList = new ArrayList<double[]>();

		// In Seconds
		double time = timeList.get(0).getTimeInMillis() / 1E3;
		// Ambiguity State Map
		HashMap<String, double[]> ambStateMap = new HashMap<String, double[]>();
		for (int i = 1; i < SVlist.size(); i++) {

			System.out.println("Step/Itr - " + i);
			ArrayList<Satellite> SV = SVlist.get(i);
			int SVcount = SV.size();
			SimpleMatrix x = kfObj.getState();
			SimpleMatrix P = kfObj.getCovariance();
			SimpleMatrix _x = new SimpleMatrix(5 + SVcount, 1);
			SimpleMatrix _P = new SimpleMatrix(5 + SVcount, 5 + SVcount);
			for (int j = 0; j < 5; j++) {
				_x.set(j, x.get(j));
				_P.set(j, j, P.get(j, j));
			}
			for (int j = 0; j < SVcount; j++) {
				Satellite sat = SV.get(j);
				String svid = String.valueOf(sat.getSSI()) + String.valueOf(sat.getSVID());
				double xVal = 0;
				double pVal = 400;
				if (ambStateMap.containsKey(svid) && sat.isLocked() == true) {
					double[] xp = ambStateMap.get(svid);
					xVal = xp[0];
					pVal = xp[1];
				} else {
					// PR and CP prealignment

					double PR = sat.getPseudorange();
					double CP = sat.getCarrier_wavelength() * sat.getCycle();

					xVal = CP - PR;

				}
				_x.set(5 + j, xVal);
				_P.set(5 + j, 5 + j, pVal);

			}
			kfObj.setState(_x, _P);
			double currentTime = timeList.get(i).getTimeInMillis() / 1E3;
			double deltaT = (int) (currentTime - time);
			if (isStatic) {
				System.out.println("NOTHING HERE");
			} else {
				runFilterDynamicPPP(deltaT, SV, timeList.get(i));
			}
			x = kfObj.getState();
			P = kfObj.getCovariance();

			double[] estECEF = new double[] { x.get(0), x.get(1), x.get(2) };
			ecefList.add(estECEF);
			for (int j = 0; j < SVcount; j++) {
				Satellite sat = SV.get(j);
				String svid = String.valueOf(sat.getSSI()) + String.valueOf(sat.getSVID());
				if (sat.isLocked() == true) {
					double xVal = x.get(5 + j);
					double pVal = P.get(5 + j, 5 + j);
					ambStateMap.put(svid, new double[] { xVal, pVal });

				} else {
					if (ambStateMap.containsKey(svid)) {
						ambStateMap.remove(svid);
					}
				}
			}
			if (!MatrixFeatures_DDRM.isPositiveDefinite(P.getMatrix())) {

				System.err.println("PositiveDefinite test Failed");
				return null;
			}
			if (isStatic) {
				double err = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> estECEF[j] - trueUserECEF[j])
						.map(j -> j * j).reduce(0, (j, k) -> j + k));
				System.out.println("postEstErr -" + P.toString());

				System.out.println("Pos Error - " + err);

				double TraceOfP = P.trace();
				System.out.println(" Trace of ErrEstCov - " + TraceOfP);

			} else {
				for (int j = 0; j < SVcount; j++) {
					String svid = String.valueOf(SV.get(j).getSSI()) + String.valueOf(SV.get(j).getSVID());
					System.out.println(svid + "  -  " + x.get(5 + j));
				}
				double[] estLLH = ECEFtoLatLon.ecef2lla(estECEF);
				double err = LatLonUtil.getHaversineDistance(trueLLHlist.get(i), estLLH);
				System.out.println("Pos Error - " + err);
			}
			time = currentTime;

		}
//		chartPack("Position Error", path + "_err.PNG", errList, timeList, 100);
//		chartPack("Error Covariance", path + "_cov.PNG", errCovList, timeList, 100);

		System.out.println(kfObj.getState().toString());
		return ecefList;

	}

	public ArrayList<double[]> iterateSPP(boolean isStatic, ArrayList<double[]> trueLLHlist) {
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
			if (isStatic) {
				runFilterStatic(deltaT, SV, timeList.get(i));
			} else {
				runFilterDynamicSPP(deltaT, SV, timeList.get(i));
			}
			SimpleMatrix x = kfObj.getState();
			SimpleMatrix P = kfObj.getCovariance();
			double[] estECEF = new double[] { x.get(0), x.get(1), x.get(2) };
			ecefList.add(estECEF);
			if (!MatrixFeatures_DDRM.isPositiveDefinite(P.getMatrix())) {
				System.out.println("PositiveDefinite test Failed");
			}
			if (isStatic) {
				double err = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> estECEF[j] - trueUserECEF[j])
						.map(j -> j * j).reduce(0, (j, k) -> j + k));
				System.out.println("postEstErr -" + P.toString());

				System.out.println("Pos Error - " + err);

				double TraceOfP = P.trace();
				System.out.println(" Trace of ErrEstCov - " + TraceOfP);
				errList.add(err);
				errCovList.add(TraceOfP);
			} else {

				double[] estLLH = ECEFtoLatLon.ecef2lla(estECEF);
				double err = LatLonUtil.getHaversineDistance(trueLLHlist.get(i), estLLH);
				System.out.println("Pos Error - " + err);
			}
			time = currentTime;

		}
//		chartPack("Position Error", path + "_err.PNG", errList, timeList, 100);
//		chartPack("Error Covariance", path + "_cov.PNG", errCovList, timeList, 100);

		System.out.println(kfObj.getState().toString());
		return ecefList;
	}

	public void runFilterStatic(double deltaT, ArrayList<Satellite> SV, Calendar time) {

		int SVcount = SV.size();

		kfObj.configStaticSPP(deltaT);
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
		IntStream.range(0, SVcount).forEach(i -> R[i][i] = prObsNoiseVar);
		kfObj.update(z, R, ze, H);

	}

	public void runFilterDynamicSPP(double deltaT, ArrayList<Satellite> SV, Calendar time) {

		int SVcount = SV.size();

		kfObj.configDynamicSPP(deltaT);
		kfObj.predict();

		SimpleMatrix x = kfObj.getState();
		double[] estECEF = new double[] { x.get(0) + PCO[0], x.get(1) + PCO[1], x.get(2) + PCO[2] };
		double rxClkOff = x.get(3);// in meters
		double[][] unitLOS = SatUtil.getUnitLOS(SV, estECEF);
		// H is the Jacobian matrix of partial derivatives Observation Model(h) of with
		// respect to x
		double[][] H = new double[SVcount][4];
		IntStream.range(0, SVcount).forEach(i -> {
			IntStream.range(0, 3).forEach(j -> H[i][j] = -unitLOS[i][j]);
			H[i][3] = 1;
		});

		double[][] z = new double[SVcount][1];
		// get satellite clock offset error, Iono and Tropo errors corrected pseudorange
		WLS wls = new WLS(SV, PCO, ionoCoeff, time, ionex, geoid);
		double[] refECEF = wls.getTropoCorrECEF();
		double[] refLatLon = ECEFtoLatLon.ecef2lla(refECEF);
		double[] tropoCorrPR = satUtil.getTropoCorrPR(SV, time, refLatLon);

		IntStream.range(0, SVcount).forEach(i -> z[i][0] = tropoCorrPR[i]);
		double[][] ze = new double[SVcount][1];

		IntStream.range(0, SVcount).forEach(
				i -> ze[i][0] = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> estECEF[j] - SV.get(i).getECI()[j])
						.map(j -> j * j).reduce(0, (j, k) -> j + k)) + rxClkOff);
		double[][] R = new double[SVcount][SVcount];
		IntStream.range(0, SVcount).forEach(i -> R[i][i] = prObsNoiseVar);
		kfObj.update(z, R, ze, H);

	}

	public void runFilterDynamicPPP(double deltaT, ArrayList<Satellite> SV, Calendar time) {

		int SVcount = SV.size();

		kfObj.configurePPP(deltaT, SVcount, false);
		kfObj.predict();

		SimpleMatrix x = kfObj.getState();
		double[] estECEF = new double[] { x.get(0) + PCO[0], x.get(1) + PCO[1], x.get(2) + PCO[2] };
		double estRxClkOff = x.get(3);// In Meters - Multiplied by c
		double resTropo = x.get(4);
		double[] ambiguity = IntStream.range(0, SVcount).mapToDouble(i -> x.get(5 + i)).toArray();
		double[] estLatLon = ECEFtoLatLon.ecef2lla(estECEF);

		ComputeTropoCorr estTropo = new ComputeTropoCorr(estLatLon, time, geoid);
		double[][] z = new double[2 * SVcount][1];
		double[][] ze = new double[2 * SVcount][1];
		double[][] H = new double[2 * SVcount][5 + SVcount];
		double[][] R = new double[2 * SVcount][2 * SVcount];
		for (int i = 0; i < SVcount; i++) {
			Satellite sat = SV.get(i);
			double[] satECI = sat.getECI();
			double E = ComputeEleAzm.computeEleAzm(estECEF, satECI)[0];
			double M_wet = estTropo.getM_wet(E);

			if (sat.getCycle() == 0 || sat.getPseudorange() == 0 || sat.getCarrier_wavelength() == 0) {
				System.err.println("ERROR - NULL CP OR PR is being used in SF PPP dyanmic kalman computation");
			}

			double[] unitLOS = SatUtil.getUnitLOS(satECI, estECEF);

			for (int j = i * 2; j <= (i * 2) + 1; j++) {
				final int _j = j;
				IntStream.range(0, 3).forEach(k -> H[_j][k] = -unitLOS[k]);
				H[j][3] = 1;
				H[j][4] = M_wet;
			}
			H[(i * 2) + 1][5 + i] = 1;
			z[2 * i][0] = sat.getPseudorange();
			z[(2 * i) + 1][0] = sat.getCarrier_wavelength() * sat.getCycle();
			double approxPR = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> estECEF[j] - satECI[j]).map(j -> j * j)
					.reduce(0, (j, k) -> j + k)) + estRxClkOff + (M_wet * resTropo);
			double approxCP = approxPR + ambiguity[i];

			ze[2 * i][0] = approxPR;
			ze[(2 * i) + 1][0] = approxCP;
			R[2 * i][2 * i] = prObsNoiseVar;// Math.pow(sat.getPrUncM(), 2);
			R[(2 * i) + 1][(2 * i) + 1] = cpObsNoiseVar;// Math.pow(sat.getGnssLog().getAdrUncM() * 100, 2);

		}

		kfObj.update(z, R, ze, H);

	}

	public void runFilter2(double deltaT, ArrayList<Satellite> SV, double[] PCO, Calendar time) {
		double[][] H = new double[4][5];
		IntStream.range(0, 4).forEach(i -> H[i][i] = 1);
		H[3][4] = deltaT;
		kfObj.configStaticSPP(deltaT);
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

		kfObj.configStaticSPP(deltaT);
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
		IntStream.range(0, SVcount).forEach(i -> R[i][i] = prObsNoiseVar);
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
