package com.RINEX_parser.ComputeUserPos.KalmanFilter.DualFreq;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.jfree.ui.RefineryUtilities;
import org.orekit.models.earth.Geoid;

import com.RINEX_parser.ComputeUserPos.KalmanFilter.Models.StaticKF;
import com.RINEX_parser.helper.ComputeEleAzm;
import com.RINEX_parser.helper.ComputeTropoCorr;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.GraphPlotter;
import com.RINEX_parser.utility.SatUtil;

public class StaticEKF {

	private StaticKF kfObj;
	private ArrayList<ArrayList<Satellite>[]> dualSVlist;
	private double[] trueUserECEF;

	private static final double SpeedofLight = 299792458;
	private double prObsNoiseVar;
	private double cpObsNoiseVar;
	private ArrayList<Calendar> timeList;

	// For Tropo Corr PR, as Orekit Geoid requires LLH
	private double[] refECEF;
	private double[] refLatLon;
	private SatUtil satUtil;

	private double[] PCO;
	private double[] fSq;
	private Geoid geoid;
	private boolean usePhase;

	public StaticEKF(ArrayList<ArrayList<Satellite>[]> dualSVlist, double[][] PCO, double[] refECEF,
			double[] trueUserECEF, Geoid geoid, ArrayList<Calendar> timeList, boolean usePhase) {
		kfObj = new StaticKF();
		this.dualSVlist = dualSVlist;
		fSq = new double[2];
		fSq[0] = Math.pow(dualSVlist.get(0)[0].get(0).getCarrier_frequency(), 2);
		fSq[1] = Math.pow(dualSVlist.get(0)[1].get(0).getCarrier_frequency(), 2);
		this.trueUserECEF = trueUserECEF;
		this.geoid = geoid;
		this.timeList = timeList;
		this.refECEF = refECEF;
		this.refLatLon = ECEFtoLatLon.ecef2lla(refECEF);
		satUtil = new SatUtil(PCO, geoid);

		this.PCO = getIonoFree(PCO);
		this.usePhase = usePhase;
		// Setting matrix R(Measurement noise) value
		prObsNoiseVar = 9;
		cpObsNoiseVar = 9e-4;
		double[][] x = null;
		double[][] P = null;
		if (usePhase) {
			x = new double[][] { { refECEF[0] }, { refECEF[1] }, { refECEF[2] }, { 0.1 }, { 0.001 }, { 0.5 } };
			P = new double[6][6];
			P[5][5] = 0.25;

		} else {
			x = new double[][] { { refECEF[0] }, { refECEF[1] }, { refECEF[2] }, { 0.1 }, { 0.001 } };
			P = new double[5][5];

		}
		for (int i = 0; i < 3; i++) {
			P[i][i] = 16;
		}
		P[3][3] = 9e10;
		P[4][4] = 1e13;

		double[][] _x = x;
		double[][] _P = P;
		System.out.println("Intial State Estimate - " + IntStream.range(0, x.length)
				.mapToObj(i -> String.valueOf(_x[i][0])).reduce("", (i, j) -> i + " " + j));

		System.out.println("Intial Estimate Error Covariance - " + IntStream.range(0, P.length)
				.mapToObj(i -> String.valueOf(_P[i][i])).reduce("", (i, j) -> i + " " + j));

		kfObj.setState(x, P);
	}

	public ArrayList<double[]> compute() {

		if (usePhase) {
			return compute2();
		}
		return compute1();

	}

	// SPP
	public ArrayList<double[]> compute1() {

		ArrayList<double[]> ecefList = new ArrayList<double[]>();
		ArrayList<Double> errList = new ArrayList<Double>();
		ArrayList<Double> errCovList = new ArrayList<Double>();
		// In Seconds
		double time = timeList.get(0).getTimeInMillis() / 1E3;

		for (int i = 1; i < dualSVlist.size(); i++) {

			System.out.println("Step/Itr - " + i);
			ArrayList<Satellite>[] SV = dualSVlist.get(i);

//			int SVcount = SV[0].size();
//			SimpleMatrix p = kfObj.getCovariance();
//			SimpleMatrix _P = new SimpleMatrix(5, 5);
//			for (int j = 0; j < 3; j++) {
//
//				_P.set(j, j, p.get(j, j));
//			}
//			_P.set(3, 4, p.get(3, 4));
//			_P.set(4, 3, p.get(4, 3));
//			kfObj.setState(kfObj.getState(), _P);

			double currentTime = timeList.get(i).getTimeInMillis() / 1E3;
			double deltaT = (int) (currentTime - time);
			runFilter2(deltaT, SV, timeList.get(i));
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

		System.out.println(kfObj.getState().toString());
		return ecefList;
	}

	// PPP
	public ArrayList<double[]> compute2() {

		ArrayList<double[]> ecefList = new ArrayList<double[]>();
		ArrayList<Double> errList = new ArrayList<Double>();
		ArrayList<Double> errCovList = new ArrayList<Double>();
		// In Seconds
		double time = timeList.get(0).getTimeInMillis() / 1E3;

		HashMap<Integer, Integer> order = new HashMap<Integer, Integer>();
		for (int i = 1; i < dualSVlist.size(); i++) {

//			for (int i = 0; i < SVcount; i++) {
//				if (SV[0].get(i).isLocked() == false || SV[1].get(i).isLocked() == false) {
//					Q[6 + i][6 + i] = 1e13 / c2;
//
//				} else {
//					System.out.print("");
//				}
//			}

			System.out.println("Step/Itr - " + i);
			ArrayList<Satellite>[] SV = dualSVlist.get(i);
			int SVcount = SV[0].size();
			HashMap<Integer, Integer> cOrder = new HashMap<Integer, Integer>();
			SimpleMatrix x = kfObj.getState();
			SimpleMatrix P = kfObj.getCovariance();
			SimpleMatrix _x = new SimpleMatrix(6 + SVcount, 1);
			SimpleMatrix _P = new SimpleMatrix(6 + SVcount, 6 + SVcount);
			for (int j = 0; j < 6; j++) {
				_x.set(j, x.get(j));
				_P.set(j, j, P.get(j, j));
			}
			_P.set(3, 4, P.get(3, 4));
			_P.set(4, 3, P.get(4, 3));
			for (int j = 0; j < SVcount; j++) {

				int svid = SV[0].get(j).getSVID();
				double xVal = 0;
				double pVal = 400;
				if (order.containsKey(svid) && SV[0].get(j).isLocked() == true) {
					int k = order.get(svid);
					xVal = x.get(6 + k);
					pVal = P.get(6 + k, 6 + k);
				} else {
					// PR and CP prealignment
					Satellite sat1 = SV[0].get(j);
					Satellite sat2 = SV[1].get(j);
					double ifPR = getIonoFree(sat1.getPseudorange(), sat2.getPseudorange());
					double ifCP = getIonoFree(sat1.getCycle() * sat1.getCarrier_wavelength(),
							sat2.getCycle() * sat2.getCarrier_wavelength());
					xVal = ifCP - ifPR;
					System.out.println();
				}
				_x.set(6 + j, xVal);
				_P.set(6 + j, 6 + j, pVal);
				cOrder.put(svid, j);
			}
			order.clear();
			order.putAll(cOrder);
			kfObj.setState(_x, _P);
			double currentTime = timeList.get(i).getTimeInMillis() / 1E3;
			double deltaT = (int) (currentTime - time);
			runFilter3(deltaT, SV, timeList.get(i));
			x = kfObj.getState();
			P = kfObj.getCovariance();

			double[] estECEF = new double[] { x.get(0), x.get(1), x.get(2) };
			ecefList.add(estECEF);
			double err = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> estECEF[j] - trueUserECEF[j]).map(j -> j * j)
					.reduce(0, (j, k) -> j + k));
			System.out.println("State -" + x.extractMatrix(0, 6, 0, 1).toString());
			for (int j = 0; j < SVcount; j++) {
				int svid = SV[0].get(j).getSVID();
				System.out.println(svid + "  -  " + x.get(6 + j));
			}
			if (!MatrixFeatures_DDRM.isPositiveDefinite(P.getMatrix())) {
				System.out.println("PositiveDefinite test Failed");
			}
			System.out.println("Pos Error - " + err);

			double TraceOfP = P.trace();

			errList.add(err);
			errCovList.add(TraceOfP);
			time = currentTime;

		}

		return ecefList;
	}

	// SPP Iterated EKF
	public void runFilter1(double deltaT, ArrayList<Satellite>[] SV, Calendar time) {

		int SVcount = SV[0].size();

		kfObj.configureSPP(deltaT);
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
		IntStream.range(0, SVcount).forEach(i -> R[i][i] = prObsNoiseVar);
		kfObj.update(z, R, ze, satECI, PCO);

	}

	// SPP EKF
	public void runFilter2(double deltaT, ArrayList<Satellite>[] SV, Calendar time) {

		int SVcount = SV[0].size();

		kfObj.configureSPP(deltaT);
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
		double[][] unitLOS = SatUtil.getUnitLOS(satECI, estECEF);
		double[][] H = new double[SVcount][5];
		IntStream.range(0, SVcount).forEach(j -> {
			IntStream.range(0, 3).forEach(k -> H[j][k] = -unitLOS[j][k]);
			H[j][3] = 1;
		});

		double[][] ze = new double[SVcount][1];

		IntStream.range(0, SVcount)
				.forEach(i -> ze[i][0] = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> estECEF[j] - satECI[i][j])
						.map(j -> j * j).reduce(0, (j, k) -> j + k)) + (estECEF[3]));
		double[][] R = new double[SVcount][SVcount];// Weight.computeCovMat(SV);
		IntStream.range(0, SVcount).forEach(i -> R[i][i] = prObsNoiseVar);// prObsNoiseVar + (prObsNoiseVar *
																			// Math.pow(Math.cos(SV[0].get(i).getElevAzm()[0]),
																			// 2)));
		kfObj.update(z, R, ze, H);

	}

	// PPP EKF
	public void runFilter3(double deltaT, ArrayList<Satellite>[] SV, Calendar time) {

		int SVcount = SV[0].size();

		kfObj.configurePPP(deltaT, SV);
		kfObj.predict();

		SimpleMatrix x = kfObj.getState();
		double[] estECEF = new double[] { x.get(0) + PCO[0], x.get(1) + PCO[1], x.get(2) + PCO[2] };
		double estRxClkOff = x.get(3);// In Meters - Multiplied by c
		double resTropo = x.get(5);
		double[] ambiguity = IntStream.range(0, SVcount).mapToDouble(i -> x.get(6 + i)).toArray();
		double[] estLatLon = ECEFtoLatLon.ecef2lla(estECEF);
		double[][] z = new double[2 * SVcount][1];

		ComputeTropoCorr tropo = new ComputeTropoCorr(refLatLon, time, geoid);
		ComputeTropoCorr estTropo = new ComputeTropoCorr(estLatLon, time, geoid);
		double[][] ze = new double[2 * SVcount][1];
		ArrayList<double[]> EleAzm = (ArrayList<double[]>) SV[0].stream().map(i -> i.getElevAzm())
				.collect(Collectors.toList());
		double[][] H = new double[2 * SVcount][6 + SVcount];
		double[][] R = new double[2 * SVcount][2 * SVcount];
		for (int i = 0; i < SVcount; i++) {
			Satellite sat1 = SV[0].get(i);
			Satellite sat2 = SV[1].get(i);
			double[] satECI = getIonoFree(sat1.getECI(), sat2.getECI());
			double tropoCorr = tropo.getSlantDelay(EleAzm.get(i)[0]);
			double E = ComputeEleAzm.computeEleAzm(estECEF, satECI)[0];
			double M_wet = estTropo.getM_wet(E);

			if (sat1.getCycle() == 0 || sat2.getCycle() == 0 || sat1.getPseudorange() == 0 || sat2.getPseudorange() == 0
					|| sat1.getCarrier_wavelength() == 0 || sat2.getCarrier_wavelength() == 0) {
				System.err.println("ERROR - NULL CP OR PR is being used in DF computation");
			}

			double corr1 = (sat1.getSatClkOff() * SpeedofLight) - tropoCorr;
			double corr2 = (sat2.getSatClkOff() * SpeedofLight) - tropoCorr;
			double IFtropoCorrPR = getIonoFree(sat1.getPseudorange() + corr1, sat2.getPseudorange() + corr2);
			double IFtropoCorrCP = getIonoFree(
					(sat1.getCycle() * sat1.getCarrier_wavelength()) + corr1 - sat1.getPhaseWindUp(),
					(sat2.getCycle() * sat2.getCarrier_wavelength()) + corr2 - sat2.getPhaseWindUp());

			double[] unitLOS = SatUtil.getUnitLOS(satECI, estECEF);

			for (int j = i * 2; j <= (i * 2) + 1; j++) {
				final int _j = j;
				IntStream.range(0, 3).forEach(k -> H[_j][k] = -unitLOS[k]);
				H[j][3] = 1;
				H[j][5] = M_wet;
			}
			H[(i * 2) + 1][6 + i] = 1;
			z[2 * i][0] = IFtropoCorrPR;
			z[(2 * i) + 1][0] = IFtropoCorrCP;
			double approxPR = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> estECEF[j] - satECI[j]).map(j -> j * j)
					.reduce(0, (j, k) -> j + k)) + estRxClkOff + (M_wet * resTropo);
			double approxCP = approxPR + ambiguity[i];

			ze[2 * i][0] = approxPR;
			ze[(2 * i) + 1][0] = approxCP;
			R[2 * i][2 * i] = prObsNoiseVar;
			R[(2 * i) + 1][(2 * i) + 1] = cpObsNoiseVar;
			System.out.print("");
		}

		kfObj.update(z, R, ze, H);

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
