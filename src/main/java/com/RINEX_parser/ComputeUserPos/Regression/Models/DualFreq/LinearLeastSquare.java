package com.RINEX_parser.ComputeUserPos.Regression.Models.DualFreq;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;
import org.orekit.models.earth.Geoid;

import com.RINEX_parser.helper.ComputeAzmEle;
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
	private ArrayList<double[]> AzmEle;
	// Regional GPS time
	private Calendar time = null;
	private double[][] PCO = null;

	public LinearLeastSquare(ArrayList<Satellite>[] SV, double[][] PCO, double[] refECEF, Calendar time) {
		this.SV = SV;
		this.PCO = PCO;
		this.time = time;
		this.alpha = new double[2];
		alpha[0] = 40 * 3 * 1E16 / Math.pow(SV[0].get(0).getCarrier_frequency(), 2);
		alpha[1] = 40 * 3 * 1E16 / Math.pow(SV[1].get(0).getCarrier_frequency(), 2);
		this.refECEF = refECEF;

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
		process(PR, Weight);
	}

	public void process(double[][] PR, double[][] Weight) {
		int SVcount = SV[0].size();
		double error = Double.MAX_VALUE;
		// Get Millimeter Accuracy, actually it takes atleast 5 iterations to converge
		// to the result which is accurate more than micrometer scale
		double threshold = 1e-3;
		if (SVcount >= 5) {

			while (error >= threshold) {
				double[][] rxAPC = new double[2][];
				for (int i = 0; i < 2; i++) {
					for (int j = 0; j < 3; j++) {
						rxAPC[i][j] = estECEF[j] + PCO[i][j];
					}

				}
				double[][] deltaPR = new double[2 * SVcount][1];
				// approx Geometric range or true range
				double[] approxGR = new double[2 * SVcount];
				double[][] coeffA = new double[2 * SVcount][5 + SVcount];
				for (int i = 0; i < 2; i++) {
					for (int j = 0; j < SVcount; j++) {

						double[] satECI = SV[i].get(j).getECI();
						final int _i = i;
						double ApproxGR = Math.sqrt(IntStream.range(0, 3).mapToDouble(k -> satECI[k] - rxAPC[_i][k])
								.map(k -> Math.pow(k, 2)).reduce((k, l) -> k + l).getAsDouble());
						final int index = (i * SVcount) + j;
						approxGR[index] = ApproxGR;

						double approxPR = approxGR[index] + (SpeedofLight * approxRcvrClkOff[i]) + (alpha[i] * STEC[j]);
						deltaPR[index][0] = PR[i][j] - approxPR;

						IntStream.range(0, 3).forEach(k -> coeffA[index][k] = -(satECI[k] - rxAPC[_i][k]) / ApproxGR);
						coeffA[index][3] = SpeedofLight;
						coeffA[index][4] = SpeedofLight;
						coeffA[index][5 + j] = alpha[i];
					}
				}
				SimpleMatrix H = new SimpleMatrix(coeffA);
				SimpleMatrix Ht = H.transpose();
				SimpleMatrix W = new SimpleMatrix(Weight);
				SimpleMatrix HtWHinv = (Ht.mult(W).mult(H)).invert();
				SimpleMatrix DeltaPR = new SimpleMatrix(deltaPR);
				SimpleMatrix DeltaX = HtWHinv.mult(Ht).mult(W).mult(DeltaPR);
				IntStream.range(0, 3).forEach(x -> estECEF[x] = estECEF[x] + DeltaX.get(x, 0));
				IntStream.range(3, 5)
						.forEach(x -> approxRcvrClkOff[x - 3] = approxRcvrClkOff[x - 3] + DeltaX.get(x, 0));
				IntStream.range(5, 5 + SVcount).forEach(x -> STEC[x - 5] = STEC[x - 5] + DeltaX.get(x, 0));

				error = Math.sqrt(IntStream.range(0, 3).mapToDouble(i -> Math.pow(DeltaX.get(i, 0), 2)).reduce(0,
						(i, j) -> i + j));

			}

			return;
		}
		System.out.println("Satellite count is less than 4, can't compute user position");

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
		double[][] PR = new double[2][];
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
		double[][] PR = getPR();
		if (time == null) {
			System.out.println("ERROR: Time info is unavailable to compute tropo corrections");

			return PR;
		}
		double[] refLatLon = ECEFtoLatLon.ecef2lla(refECEF);
		ArrayList<double[]> AzmEle = getAzmEle();
		int SVcount = PR.length;
		ComputeTropoCorr tropo = new ComputeTropoCorr(refLatLon, time, geoid);
		double[] tropoCorr = IntStream.range(0, SVcount).mapToDouble(x -> tropo.getSlantDelay(AzmEle.get(x)[0]))
				.toArray();

		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < SVcount; j++) {
				PR[i][j] = PR[i][j] - tropoCorr[j];
			}
		}
		return PR;

	}

	public ArrayList<double[]> getAzmEle() {
		if (Optional.ofNullable(AzmEle).isPresent()) {
			return AzmEle;
		}
		setAzmEle();
		return AzmEle;
	}

	public void setAzmEle() {
		AzmEle = (ArrayList<double[]>) SV[0].stream().map(i -> ComputeAzmEle.computeAzmEle(refECEF, i.getECEF()))
				.collect(Collectors.toList());
	}

	public ArrayList<Satellite>[] getSV() {
		return SV;
	}

}
