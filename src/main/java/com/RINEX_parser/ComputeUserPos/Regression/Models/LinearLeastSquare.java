package com.RINEX_parser.ComputeUserPos.Regression.Models;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;

import com.RINEX_parser.helper.ComputeAzmEle;
import com.RINEX_parser.helper.ComputeIonoCorr;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.ECEFtoLatLon;

public class LinearLeastSquare {

	private final static double SpeedofLight = 299792458;
	private double[] estECEF = new double[] { 0, 0, 0 };
	private SimpleMatrix HtWHinv = null;
	private double approxUserClkOff = 0;
	private IonoCoeff ionoCoeff;
	private ArrayList<Satellite> SV;
	private double[][] Weight;
	private double[] refLatLon;
	private ArrayList<double[]> AzmEle;

	public LinearLeastSquare(ArrayList<Satellite> SV, IonoCoeff ionoCoeff) {
		this.SV = SV;
		this.ionoCoeff = ionoCoeff;

	}

	public LinearLeastSquare(ArrayList<Satellite> SV) {
		this.SV = SV;
	}

	public void estimate(double[] PR) {
		estimate(PR, Weight);
	}

	public void intialize() {
		estECEF = new double[] { 0, 0, 0 };
		approxUserClkOff = 0;
		HtWHinv = null;
	}

	public void estimate(double[] PR, double[][] Weight) {
		intialize();
		int SVcount = SV.size();
		if (SVcount >= 4) {

			for (int i = 0; i < 100; i++) {
				double[][] deltaPR = new double[SVcount][1];
				// approx Geometric range or true range
				double[] approxGR = new double[SVcount];
				double[][] coeffA = new double[SVcount][4];
				for (int j = 0; j < SVcount; j++) {

					double[] satECEF = SV.get(j).getECEF();

					double ApproxGR = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> satECEF[x] - estECEF[x])
							.map(x -> Math.pow(x, 2)).reduce((x, y) -> x + y).getAsDouble());
					approxGR[j] = ApproxGR;

					double approxPR = approxGR[j] + (SpeedofLight * approxUserClkOff);
					deltaPR[j][0] = approxPR - PR[j];
					int index = j;
					IntStream.range(0, 3).forEach(x -> coeffA[index][x] = (satECEF[x] - estECEF[x]) / ApproxGR);
					coeffA[j][3] = 1;
				}
				SimpleMatrix H = new SimpleMatrix(coeffA);
				SimpleMatrix Ht = H.transpose();
				SimpleMatrix W = new SimpleMatrix(Weight);
				HtWHinv = (Ht.mult(W).mult(H)).invert();
				SimpleMatrix DeltaPR = new SimpleMatrix(deltaPR);
				SimpleMatrix DeltaX = HtWHinv.mult(Ht).mult(W).mult(DeltaPR);
				IntStream.range(0, 3).forEach(x -> estECEF[x] = estECEF[x] + DeltaX.get(x, 0));
				approxUserClkOff += (-DeltaX.get(3, 0)) / SpeedofLight;

			}

			return;
		}
		System.out.println("Satellite count is less than 4, can't compute user position");

	}

	public double[] getEstECEF() {
		return estECEF;
	}

	public double getRcvrClkOff() {
		return approxUserClkOff;
	}

	public SimpleMatrix getCovdX() {
		return HtWHinv;
	}

	public double[] getPR() {
		// Removed satellite clock offset error from pseudorange
		double[] PR = SV.stream().mapToDouble(x -> x.getPseudorange() + (SpeedofLight * x.getSatClkOff())).toArray();
		return PR;
	}

	public double[] getIonoCorrPR() {
		if (Optional.ofNullable(ionoCoeff).isEmpty()) {
			System.out.println("You have not provided IonoCoeff");
			return null;
		}
		ArrayList<double[]> AzmEle = getAzmEle();
		double[] PR = getPR();
		double[] ionoCorr = IntStream.range(0, SV.size())
				.mapToDouble(x -> ComputeIonoCorr.computeIonoCorr(AzmEle.get(x)[0], AzmEle.get(x)[1], refLatLon[0],
						refLatLon[1], (long) SV.get(x).gettSV(), ionoCoeff))
				.toArray();

//		System.out.println("IONO corrections");
//		IntStream.range(0, ionoCorr.length)
//				.forEach(i -> System.out.print("GPS" + SV.get(i).getSVID() + " - " + ionoCorr[i] + " "));
//
//		System.out.println("");

		return IntStream.range(0, PR.length).mapToDouble(x -> PR[x] - ionoCorr[x]).toArray();
	}

	public ArrayList<double[]> getAzmEle() {
		if (Optional.ofNullable(AzmEle).isPresent()) {
			return AzmEle;
		}
		int SVcount = SV.size();
		double[][] Weight = new double[SVcount][SVcount];
		IntStream.range(0, SVcount).forEach(i -> Weight[i][i] = 1);
		estimate(getPR(), Weight);
		double[] refECEF = estECEF;
		refLatLon = ECEFtoLatLon.ecef2lla(refECEF);

		// After we get the user ECEF, we will be able to calculate Azm&Ele angle which
		// will in
		// turn help us to calculate and remove iono error
		AzmEle = (ArrayList<double[]>) SV.stream().map(i -> ComputeAzmEle.computeAzmEle(refECEF, i.getECEF()))
				.collect(Collectors.toList());
		return AzmEle;
	}

	public double[][] getWeight() {
		return Weight;
	}

	public void setWeight(double[][] Weight) {
		this.Weight = Weight;
	}
}