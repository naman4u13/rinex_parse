package com.RINEX_parser.ComputeUserPos.Regression.Models.DualFreq;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
import org.orekit.models.earth.Geoid;

import com.RINEX_parser.constants.Constellation;
import com.RINEX_parser.helper.ComputeTropoCorr;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.ECEFtoLatLon;

public class LinearLeastSquare {

	private final static double SpeedofLight = 299792458;
	private double[] estECEF = null;
	private double[] approxRcvrClkOff = null;
	// Slant TEC
	private double[] STEC = null;
	private double[] alpha = null;
	private ArrayList<Satellite>[] SV;
	private double[][] Weight;
	private double[] refECEF;
	private ArrayList<double[]> EleAzm;
	private double[][] PR;
	private double[][] tropoCorrPR;
	// Regional GPS time
	private Calendar time = null;
	private double[][] PCO = null;
	private double[] fSq;

	public LinearLeastSquare(ArrayList<Satellite>[] SV, double[][] PCO, double[] refECEF, Calendar time) {
		this.SV = SV;
		this.PCO = PCO;
		this.time = time;
		this.alpha = new double[2];
		fSq = new double[2];
		fSq[0] = Math.pow(SV[0].get(0).getCarrier_frequency(), 2);
		fSq[1] = Math.pow(SV[1].get(0).getCarrier_frequency(), 2);
		alpha[0] = 40.3 * 1E16 / fSq[0];
		alpha[1] = 40.3 * 1E16 / fSq[1];
		this.refECEF = refECEF;
		check();

	}

	public void estimate(double[][] PR) {
		estimate(PR, Weight);
	}

	public void intialize() {
		estECEF = new double[] { 0, 0, 0 };
		approxRcvrClkOff = new double[] { 0, 0 };
		STEC = new double[SV[0].size()];

	}

	public void estimate(double[][] PR, double[][] Weight) {
		intialize();
		process3(PR, Weight);
	}

//	public void process(double[][] PR, double[][] Weight) {
//		int SVcount = SV[0].size();
//		double error = Double.MAX_VALUE;
//		// Get Millimeter Accuracy, actually it takes atleast 5 iterations to converge
//		// to the result which is accurate more than micrometer scale
//		double threshold = 1e-3;
//		if (SVcount >= 5) {
//
//			while (error >= threshold) {
//				double[][] rxAPC = new double[2][3];
//				for (int i = 0; i < 2; i++) {
//					for (int j = 0; j < 3; j++) {
//						rxAPC[i][j] = estECEF[j] + PCO[i][j];
//					}
//
//				}
//				double[][] deltaPR = new double[2 * SVcount][1];
//				// approx Geometric range or true range
//				double[] approxGR = new double[2 * SVcount];
//				double[][] coeffA = new double[2 * SVcount][5 + SVcount];
//				for (int i = 0; i < 2; i++) {
//					for (int j = 0; j < SVcount; j++) {
//
//						double[] satECI = SV[i].get(j).getECI();
//						final int _i = i;
//						double ApproxGR = Math.sqrt(IntStream.range(0, 3).mapToDouble(k -> satECI[k] - rxAPC[_i][k])
//								.map(k -> Math.pow(k, 2)).reduce((k, l) -> k + l).getAsDouble());
//						final int index = (i * SVcount) + j;
//						approxGR[index] = ApproxGR;
//
//						double approxPR = approxGR[index] + (SpeedofLight * approxRcvrClkOff[i]) + (alpha[i] * STEC[j]);
//						deltaPR[index][0] = PR[i][j] - approxPR;
//
//						IntStream.range(0, 3).forEach(k -> coeffA[index][k] = -(satECI[k] - rxAPC[_i][k]) / ApproxGR);
//						coeffA[index][3 + i] = SpeedofLight;
//
//						coeffA[index][5 + j] = alpha[i];
//					}
//				}
//				SimpleMatrix H = new SimpleMatrix(coeffA);
//
//				SimpleSVD<SimpleMatrix> svd = new SimpleSVD<SimpleMatrix>(H.getMatrix(), true);
//				int rank = svd.rank();
//				double[] singularVal = svd.getSingularValues();
//				int n = singularVal.length;
//				double eps = Math.ulp(1.0);
//				double[][] _Wplus = new double[n][n];
//				double maxW = Double.MIN_VALUE;
//				for (int i = 0; i < n; i++) {
//					maxW = Math.max(singularVal[i], maxW);
//				}
//				double tolerance = Math.max(H.numRows(), H.numCols()) * eps * maxW;
//				for (int i = 0; i < n; i++) {
//					double val = singularVal[i];
//					if (val < tolerance) {
//						continue;
//					}
//					_Wplus[i][i] = 1 / val;
//				}
//				SimpleMatrix Wplus = new SimpleMatrix(_Wplus);
//				Wplus = Wplus.transpose();
//				SimpleMatrix U = svd.getU();
//
//				SimpleMatrix V = svd.getV();
//
////				SimpleMatrix Ht = H.transpose();
////				SimpleMatrix W = new SimpleMatrix(Weight);
////
////				SimpleMatrix HtWHinv = (Ht.mult(W).mult(H)).invert();
//				SimpleMatrix DeltaPR = new SimpleMatrix(deltaPR);
////				SimpleMatrix DeltaX = HtWHinv.mult(Ht).mult(W).mult(DeltaPR);
//
//				SimpleMatrix Ut = U.transpose();
//				SimpleMatrix DeltaX = V.mult(Wplus).mult(Ut).mult(DeltaPR);
//				IntStream.range(0, 3).forEach(x -> estECEF[x] = estECEF[x] + DeltaX.get(x, 0));
//				IntStream.range(3, 5)
//						.forEach(x -> approxRcvrClkOff[x - 3] = approxRcvrClkOff[x - 3] + DeltaX.get(x, 0));
//				IntStream.range(5, 5 + SVcount).forEach(x -> STEC[x - 5] = STEC[x - 5] + DeltaX.get(x, 0));
//
//				error = Math.sqrt(IntStream.range(0, 3).mapToDouble(i -> Math.pow(DeltaX.get(i, 0), 2)).reduce(0,
//						(i, j) -> i + j));
//
//			}
//
//			return;
//		}
//		System.out.println("Satellite count is less than 4, can't compute user position");
//
//	}

//	public void process2(double[][] _PR, double[][] Weight) {
//		int SVcount = SV[0].size();
//		double error = Double.MAX_VALUE;
//		// iono free rxPCO
//		double[] PCO = getIonoFree(this.PCO);
//		double[][] satECI = new double[SVcount][3];
//		double[] PR = new double[SVcount];
//		double approxRcvrClkOff = 0;
//		for (int i = 0; i < SVcount; i++) {
//			satECI[i] = getIonoFree(SV[0].get(i).getECI(), SV[1].get(i).getECI());
//			PR[i] = getIonoFree(_PR[0][i], _PR[1][i]);
//		}
//
//		// Get Millimeter Accuracy, actually it takes atleast 5 iterations to converge
//		// to the result which is accurate more than micrometer scale
//		double threshold = 1e-3;
//		if (SVcount >= 4) {
//
//			while (error >= threshold) {
//				double[] rxAPC = new double[3];
//				for (int i = 0; i < 3; i++) {
//					rxAPC[i] = estECEF[i] + PCO[i];
//
//				}
//				double[][] deltaPR = new double[SVcount][1];
//				// approx Geometric range or true range
//				double[] approxGR = new double[SVcount];
//				double[][] coeffA = new double[SVcount][4];
//
//				for (int i = 0; i < SVcount; i++) {
//					final int _i = i;
//					double ApproxGR = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> satECI[_i][j] - rxAPC[j])
//							.map(j -> Math.pow(j, 2)).reduce((j, k) -> j + k).getAsDouble());
//
//					approxGR[i] = ApproxGR;
//
//					double approxPR = approxGR[i] + (SpeedofLight * approxRcvrClkOff);
//					deltaPR[i][0] = PR[i] - approxPR;
//
//					IntStream.range(0, 3).forEach(j -> coeffA[_i][j] = -(satECI[_i][j] - rxAPC[j]) / ApproxGR);
//					coeffA[i][3] = SpeedofLight;
//
//				}
//
//				SimpleMatrix H = new SimpleMatrix(coeffA);
//				SimpleMatrix Ht = H.transpose();
//				SimpleMatrix W = new SimpleMatrix(Weight);
//				SimpleMatrix HtWHinv = (Ht.mult(W).mult(H)).invert();
//				SimpleMatrix DeltaPR = new SimpleMatrix(deltaPR);
//				SimpleMatrix DeltaX = HtWHinv.mult(Ht).mult(W).mult(DeltaPR);
//				IntStream.range(0, 3).forEach(x -> estECEF[x] = estECEF[x] + DeltaX.get(x, 0));
//				approxRcvrClkOff += DeltaX.get(3, 0);
//				error = Math.sqrt(IntStream.range(0, 3).mapToDouble(i -> Math.pow(DeltaX.get(i, 0), 2)).reduce(0,
//						(i, j) -> i + j));
//
//			}
//
//			return;
//		}
//		System.out.println("Satellite count is less than 4, can't compute user position");
//
//	}

	public void check() {
		for (int i = 0; i < SV[0].size(); i++) {
			int SVID1 = SV[0].get(i).getSVID();
			int SVID2 = SV[1].get(i).getSVID();
			if (SVID1 != SVID2) {
				System.err.println("SVID didn't match FATAL ERROR in LINEAR-LEAST");
			}
		}
	}

	public void process3(double[][] PR, double[][] Weight) {

		int SVcount = SV[0].size();
		double error = Double.MAX_VALUE;
		double[] iono = new double[SVcount];
		double f1Sq = Math.pow(Constellation.frequency.get('G').get(1), 2);
		double[] mu = new double[2];
		mu[0] = f1Sq / fSq[0];
		mu[1] = f1Sq / fSq[1];
		double approxRcvrClkOff = 0;
		// Get Millimeter Accuracy, actually it takes atleast 5 iterations to converge
		// to the result which is accurate more than micrometer scale
		double threshold = 1e-3;
		if (SVcount >= 4) {

			while (error >= threshold) {
				double[][] rxAPC = new double[2][3];
				for (int i = 0; i < 2; i++) {
					for (int j = 0; j < 3; j++) {
						rxAPC[i][j] = estECEF[j] + PCO[i][j];
					}

				}
				double[][] deltaPR = new double[2 * SVcount][1];
				// approx Geometric range or true range
				double[] approxGR = new double[2 * SVcount];
				double[][] coeffA = new double[2 * SVcount][4 + SVcount];
				for (int i = 0; i < 2; i++) {
					for (int j = 0; j < SVcount; j++) {

						double[] satECI = SV[i].get(j).getECI();
						final int _i = i;
						double ApproxGR = Math.sqrt(IntStream.range(0, 3).mapToDouble(k -> satECI[k] - rxAPC[_i][k])
								.map(k -> Math.pow(k, 2)).reduce((k, l) -> k + l).getAsDouble());
						final int index = (i * SVcount) + j;
						approxGR[index] = ApproxGR;

						double approxPR = approxGR[index] + (SpeedofLight * approxRcvrClkOff) + (mu[i] * iono[j]);
						deltaPR[index][0] = PR[i][j] - approxPR;

						IntStream.range(0, 3).forEach(k -> coeffA[index][k] = -(satECI[k] - rxAPC[_i][k]) / ApproxGR);
						coeffA[index][3] = 1;

						coeffA[index][4 + j] = mu[i];

					}
				}
				SimpleMatrix H = new SimpleMatrix(coeffA);

				SimpleSVD<SimpleMatrix> svd = new SimpleSVD<SimpleMatrix>(H.getMatrix(), true);
				int rank = svd.rank();
				double[] singularVal = svd.getSingularValues();
				int n = singularVal.length;
				double eps = Math.ulp(1.0);
				double[][] _Wplus = new double[n][n];
				double maxW = Double.MIN_VALUE;
				for (int i = 0; i < n; i++) {
					maxW = Math.max(singularVal[i], maxW);
				}
				double tolerance = Math.max(H.numRows(), H.numCols()) * eps * maxW;
				for (int i = 0; i < n; i++) {
					double val = singularVal[i];
					if (val < tolerance) {
						continue;
					}
					_Wplus[i][i] = 1 / val;
				}
				SimpleMatrix Wplus = new SimpleMatrix(_Wplus);
				Wplus = Wplus.transpose();
				SimpleMatrix U = svd.getU();

				SimpleMatrix V = svd.getV();

//				SimpleMatrix Ht = H.transpose();
//				SimpleMatrix W = new SimpleMatrix(Weight);
//
//				SimpleMatrix HtWHinv = (Ht.mult(W).mult(H)).invert();
				SimpleMatrix DeltaPR = new SimpleMatrix(deltaPR);
//				SimpleMatrix DeltaX = HtWHinv.mult(Ht).mult(W).mult(DeltaPR);

				SimpleMatrix Ut = U.transpose();
				SimpleMatrix DeltaX = V.mult(Wplus).mult(Ut).mult(DeltaPR);
				IntStream.range(0, 3).forEach(x -> estECEF[x] = estECEF[x] + DeltaX.get(x, 0));
				approxRcvrClkOff += DeltaX.get(3, 0) / SpeedofLight;
				IntStream.range(4, 4 + SVcount).forEach(x -> iono[x - 4] = iono[x - 4] + DeltaX.get(x, 0));

				error = Math.sqrt(IntStream.range(0, 3).mapToDouble(i -> Math.pow(DeltaX.get(i, 0), 2)).reduce(0,
						(i, j) -> i + j));

			}

			return;
		}
		System.err.println("Satellite count is less than 4, can't compute user position");

	}

	public double[] getEstECEF() {
		return estECEF;
	}

	public double[] getRcvrClkOff() {
		return approxRcvrClkOff;
	}

	public double[] getSTEC() {
		return STEC;
	}

	public double[][] getPR() {
		return getPR(this.SV);
	}

	public double[][] getPR(ArrayList<Satellite>[] SV) {
		if (Optional.ofNullable(PR).isPresent()) {
			return PR;
		}
		PR = new double[2][];
		// Removed satellite clock offset error from pseudorange
		PR[0] = SV[0].stream().mapToDouble(i -> i.getPseudorange() + (SpeedofLight * i.getSatClkOff())).toArray();
		PR[1] = SV[1].stream().mapToDouble(i -> i.getPseudorange() + (SpeedofLight * i.getSatClkOff())).toArray();

		return PR;
	}

	public double[][] getWeight() {
		return Weight;
	}

	public void setWeight(double[][] Weight) {
		this.Weight = Weight;
	}

	public double[][] getTropoCorrPR(Geoid geoid) {
		if (Optional.ofNullable(this.tropoCorrPR).isPresent()) {
			return this.tropoCorrPR;
		}
		double[][] PR = getPR();
		if (time == null) {
			System.err.println("ERROR: Time info is unavailable to compute tropo corrections");

			return PR;
		}
		double[] refLatLon = ECEFtoLatLon.ecef2lla(refECEF);
		ArrayList<double[]> EleAzm = getEleAzm();
		int SVcount = PR[0].length;
		ComputeTropoCorr tropo = new ComputeTropoCorr(refLatLon, time, geoid);
		double[] tropoCorr = IntStream.range(0, SVcount).mapToDouble(x -> tropo.getSlantDelay(EleAzm.get(x)[0]))
				.toArray();

		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < SVcount; j++) {
				PR[i][j] = PR[i][j] - tropoCorr[j];
			}
		}
		tropoCorrPR = PR;
		return PR;

	}

	public ArrayList<double[]> getEleAzm() {
		if (Optional.ofNullable(EleAzm).isPresent()) {
			return EleAzm;
		}
		EleAzm = (ArrayList<double[]>) SV[0].stream().map(i -> i.getElevAzm()).collect(Collectors.toList());
		return EleAzm;
	}

	public ArrayList<Satellite>[] getSV() {
		return SV;
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
