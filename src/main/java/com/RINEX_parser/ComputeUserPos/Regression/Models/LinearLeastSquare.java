package com.RINEX_parser.ComputeUserPos.Regression.Models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
import org.orekit.models.earth.Geoid;

import com.RINEX_parser.fileParser.IONEX;
import com.RINEX_parser.helper.ComputeIonoCorr;
import com.RINEX_parser.helper.ComputeTropoCorr;
import com.RINEX_parser.helper.SBAS.Flag;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.LatLonUtil;

public class LinearLeastSquare {

	private final static double SpeedofLight = 299792458;
	private double[] estECEF = null;
	private SimpleMatrix HtWHinv = null;
	private double approxRcvrClkOff = 0;
	private IonoCoeff ionoCoeff;
	private ArrayList<Satellite> SV;
	private double[][] Weight;
	private double[] PR;
	private double[] ionoCorrPR;
	// Tropo corrected PR will already have iono corrections
	private double[] tropoCorrPR;
	private ArrayList<double[]> EleAzm;
	private double estVel = 0;
	private double approxRcvrClkDrift = 0;
	// Regional GPS time
	private Calendar time = null;
	private double[] PCO = null;
	private HashMap<Integer, HashMap<Integer, Double>> sbasIVD = null;
	private IONEX ionex = null;
	private Geoid geoid;

	public LinearLeastSquare(ArrayList<Satellite> SV, double[] PCO, IonoCoeff ionoCoeff, Calendar time,
			HashMap<Integer, HashMap<Integer, Double>> sbasIVD, IONEX ionex, Geoid geoid) {
		this.SV = SV;
		this.ionoCoeff = ionoCoeff;
		this.time = time;
		this.PCO = PCO;
		this.sbasIVD = sbasIVD;
		this.ionex = ionex;
		this.geoid = geoid;

	}

	public LinearLeastSquare(ArrayList<Satellite> SV, double[] PCO, IonoCoeff ionoCoeff, Calendar time, IONEX ionex,
			Geoid geoid) {
		this(SV, PCO, ionoCoeff, time, null, ionex, geoid);

	}

	public LinearLeastSquare(ArrayList<Satellite> SV, double[] PCO) {
		this(SV, PCO, null, null, null, null, null);

	}

	public LinearLeastSquare(ArrayList<Satellite> SV, double[] PCO, Calendar time) {
		this(SV, PCO, null, time, null, null, null);

	}

	public void estimate(double[] PR) {
		estimate(PR, Weight);
	}

	public void intialize() {
		estECEF = new double[] { 0, 0, 0 };
		approxRcvrClkOff = 0;
		HtWHinv = null;
	}

	public void estimate(double[] PR, double[][] Weight) {
		intialize();
		process(PR, Weight);
//		Below lines are commented out because they are computationally expensive
//		It corrects SV ECEF to ECI transformation by removing rcvr clock bias
//		SV.stream().forEach(i -> i.updateECI(approxRcvrClkOff));
//		HtWHinv = null;
//		process(PR, Weight);

	}

	public void process(double[] PR, double[][] Weight) {
		int SVcount = SV.size();
		double error = Double.MAX_VALUE;
		// Get Millimeter Accuracy, actually it takes atleast 5 iterations to converge
		// to the result which is accurate more than micrometer scale
		double threshold = 1e-3;
		if (SVcount >= 4) {

			while (error >= threshold) {
				double[] rxAPC = IntStream.range(0, 3).mapToDouble(x -> estECEF[x] + PCO[x]).toArray();
				double[][] deltaPR = new double[SVcount][1];
				// approx Geometric range or true range
				double[] approxGR = new double[SVcount];
				double[][] coeffA = new double[SVcount][4];
				for (int j = 0; j < SVcount; j++) {

					double[] satECI = SV.get(j).getECI();

					double ApproxGR = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> satECI[x] - rxAPC[x])
							.map(x -> Math.pow(x, 2)).reduce((x, y) -> x + y).getAsDouble());
					approxGR[j] = ApproxGR;

					double approxPR = approxGR[j] + (SpeedofLight * approxRcvrClkOff);
					deltaPR[j][0] = approxPR - PR[j];
					int index = j;
					IntStream.range(0, 3).forEach(x -> coeffA[index][x] = (satECI[x] - rxAPC[x]) / ApproxGR);
					coeffA[j][3] = 1;
				}
				SimpleMatrix H = new SimpleMatrix(coeffA);
				SimpleMatrix Ht = H.transpose();
				SimpleMatrix W = new SimpleMatrix(Weight);

				HtWHinv = (Ht.mult(W).mult(H)).invert();

				SimpleMatrix DeltaPR = new SimpleMatrix(deltaPR);
				SimpleMatrix DeltaX = HtWHinv.mult(Ht).mult(W).mult(DeltaPR);
				IntStream.range(0, 3).forEach(x -> estECEF[x] = estECEF[x] + DeltaX.get(x, 0));
				approxRcvrClkOff += (-DeltaX.get(3, 0)) / SpeedofLight;
				error = Math.sqrt(IntStream.range(0, 3).mapToDouble(i -> Math.pow(DeltaX.get(i, 0), 2)).reduce(0,
						(i, j) -> i + j));

			}

			return;
		}
		System.err.println("Satellite count is less than 4, can't compute user position");
		estECEF = null;

	}

	public void process2(double[] PR, double[][] Weight) {
		int SVcount = SV.size();
		double error = Double.MAX_VALUE;
		// Get Millimeter Accuracy, actually it takes atleast 5 iterations to converge
		// to the result which is accurate more than micrometer scale
		double threshold = 1e-3;
		if (SVcount >= 4) {

			while (error >= threshold) {
				double[] rxAPC = IntStream.range(0, 3).mapToDouble(x -> estECEF[x] + PCO[x]).toArray();
				double[][] deltaPR = new double[SVcount][1];
				// approx Geometric range or true range
				double[] approxGR = new double[SVcount];
				double[][] coeffA = new double[SVcount][4];
				for (int j = 0; j < SVcount; j++) {

					double[] satECI = SV.get(j).getECI();

					double ApproxGR = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> satECI[x] - rxAPC[x])
							.map(x -> Math.pow(x, 2)).reduce((x, y) -> x + y).getAsDouble());
					approxGR[j] = ApproxGR;

					double approxPR = approxGR[j] + (SpeedofLight * approxRcvrClkOff);
					deltaPR[j][0] = approxPR - PR[j];
					int index = j;
					IntStream.range(0, 3).forEach(x -> coeffA[index][x] = (satECI[x] - rxAPC[x]) / ApproxGR);
					coeffA[j][3] = 1;
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

				SimpleMatrix DeltaPR = new SimpleMatrix(deltaPR);

				SimpleMatrix Ut = U.transpose();
				SimpleMatrix DeltaX = V.mult(Wplus).mult(Ut).mult(DeltaPR);

				IntStream.range(0, 3).forEach(x -> estECEF[x] = estECEF[x] + DeltaX.get(x, 0));
				approxRcvrClkOff += (-DeltaX.get(3, 0)) / SpeedofLight;
				error = Math.sqrt(IntStream.range(0, 3).mapToDouble(i -> Math.pow(DeltaX.get(i, 0), 2)).reduce(0,
						(i, j) -> i + j));
				System.out.print("");

			}

			return;
		}
		System.out.println("Satellite count is less than 4, can't compute user position");
		estECEF = null;

	}

	public double[] getEstECEF() {
		return estECEF;
	}

	public double[] getPCO() {
		return PCO;
	}

	public double getRcvrClkOff() {
		return approxRcvrClkOff;
	}

	public SimpleMatrix getCovdX() {
		return HtWHinv;
	}

	public void setCovdX(SimpleMatrix HtWHinv) {
		this.HtWHinv = HtWHinv;
	}

	public double[] getPR() {
		return getPR(this.SV);
	}

	public double[] getPR(ArrayList<Satellite> SV) {
		if (Optional.ofNullable(PR).isPresent()) {
			return PR;
		}

		// Removed satellite clock offset error from pseudorange
		PR = SV.stream().mapToDouble(x -> x.getPseudorange() + (SpeedofLight * x.getSatClkOff())).toArray();
		return PR;
	}

	public double[] getIonoCorrPR() {
		return getIonoCorrPR(this.SV);
	}

	public double[] getIonoCorrPR(ArrayList<Satellite> SV) {
		if (Optional.ofNullable(this.ionoCorrPR).isPresent()) {
			return this.ionoCorrPR;
		}

		int SVcount = SV.size();

		ArrayList<double[]> EleAzm = getEleAzm();
		double[] PR = getPR();
		double[] ionoErr = new double[SVcount];
		double[] ionoCorrKlob = new double[SVcount];
		double[][] Weight = new double[SVcount][SVcount];
		IntStream.range(0, SVcount).forEach(i -> Weight[i][i] = 1);
		LinearLeastSquare lls = new LinearLeastSquare(SV, PCO);
		lls.estimate(getPR(), Weight);
		double[] refECEF = lls.getEstECEF();
		double[] refLatLon = ECEFtoLatLon.ecef2lla(refECEF);

		if (sbasIVD != null) {
			double[] ionoCorrSBAS = new double[SVcount];
			com.RINEX_parser.helper.SBAS.ComputeIonoCorr sbasIC = new com.RINEX_parser.helper.SBAS.ComputeIonoCorr();
			for (int i = 0; i < SVcount; i++) {
				double sbasIonoCorr = sbasIC.computeIonoCorr(EleAzm.get(i)[0], EleAzm.get(i)[1], refLatLon[0],
						refLatLon[1], sbasIVD);
				if (sbasIC.getIonoFlag() == Flag.VIABLE) {
					ionoCorrKlob[i] = ionoErr[i];
					ionoErr[i] = sbasIonoCorr;
					ionoCorrSBAS[i] = sbasIonoCorr;
				}
			}

		} else if (ionex != null) {
			// Geocentric Latitude
			double gcLat = LatLonUtil.gd2gc(refLatLon[0], refLatLon[2]);
			for (int i = 0; i < SVcount; i++) {
				double gimIonoCorr = ionex.computeIonoCorr(EleAzm.get(i)[0], EleAzm.get(i)[1], gcLat, refLatLon[1],
						SV.get(i).gettRX(), SV.get(i).getCarrier_frequency(), time);
				ionoErr[i] = gimIonoCorr;
			}

		} else {
			if (Optional.ofNullable(ionoCoeff).isEmpty()) {
				System.out.println("You have not provided IonoCoeff");
				return null;
			}
			ionoErr = IntStream.range(0, SVcount)
					.mapToDouble(x -> ComputeIonoCorr.computeIonoCorr(EleAzm.get(x)[0], EleAzm.get(x)[1], refLatLon[0],
							refLatLon[1], SV.get(x).gettRX(), ionoCoeff, SV.get(x).getCarrier_frequency(), time))
					.toArray();
		}
		for (int i = 0; i < SVcount; i++) {
			SV.get(i).setIonoErr(ionoErr[i]);
			PR[i] -= ionoErr[i];
		}
		this.ionoCorrPR = PR;
		return PR;
	}

	public double[] getTropoCorrPR() {
		if (Optional.ofNullable(this.tropoCorrPR).isPresent()) {
			return this.tropoCorrPR;
		}
		System.err.println("Tropo Corr PR is null, provide userECEF");
		return null;
	}

	public double[] getTropoCorrPR(double[] refLatLon) {
		return getTropoCorrPR(this.SV, refLatLon);
	}

	public double[] getTropoCorrPR(ArrayList<Satellite> SV, double[] refLatLon) {
		if (Optional.ofNullable(this.tropoCorrPR).isPresent()) {
			return this.tropoCorrPR;
		}
		double[] PR = getIonoCorrPR();
		if (time == null) {
			System.out.println("ERROR: Time info is unavailable to compute tropo corrections");

			return PR;
		}
		ArrayList<double[]> EleAzm = getEleAzm();

		ComputeTropoCorr tropo = new ComputeTropoCorr(refLatLon, time, geoid);
		double[] tropoErr = IntStream.range(0, SV.size()).mapToDouble(x -> tropo.getSlantDelay(EleAzm.get(x)[0]))
				.toArray();

		IntStream.range(0, SV.size()).forEach(i -> SV.get(i).setTropoErr(tropoErr[i]));
		this.tropoCorrPR = IntStream.range(0, PR.length).mapToDouble(x -> PR[x] - tropoErr[x]).toArray();
		return this.tropoCorrPR;

	}

	public ArrayList<double[]> getEleAzm() {

		return getEleAzm(this.SV);
	}

	public ArrayList<double[]> getEleAzm(ArrayList<Satellite> SV) {
		if (Optional.ofNullable(EleAzm).isPresent()) {
			return EleAzm;
		}
		EleAzm = (ArrayList<double[]>) SV.stream().map(i -> i.getElevAzm()).collect(Collectors.toList());

		return EleAzm;
	}

	public double[][] getWeight() {
		return Weight;
	}

	public void setWeight(double[][] Weight) {
		this.Weight = Weight;
	}

	public void computeRcvrInfo(boolean isStatic) {
		computeRcvrInfo(estECEF, isStatic);

	}

	public void computeRcvrInfo(double[] userECEF, boolean isStatic) {
		estVel = 0;
		approxRcvrClkDrift = 0;
		int SVcount = SV.size();

		SimpleMatrix d = new SimpleMatrix(SVcount, 1);
		SimpleMatrix unitLOS = new SimpleMatrix(SVcount, 3);
		for (int i = 0; i < SVcount; i++) {
			Satellite sat = SV.get(i);
			double[] satECI = sat.getECI();
			double ApproxGR = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> satECI[x] - userECEF[x])
					.map(x -> Math.pow(x, 2)).reduce((x, y) -> x + y).getAsDouble());
			int rowNum = i;
			IntStream.range(0, 3).forEach(j -> unitLOS.set(rowNum, j, (satECI[j] - userECEF[j]) / ApproxGR));
			double PRrate = sat.getPseudoRangeRate();
			double SatClkDrift = SpeedofLight * sat.getSatClkDrift();
			double[] SatVel = sat.getSatVel();
			double SatVelLOS = IntStream.range(0, 3).mapToDouble(j -> SatVel[j] * unitLOS.get(rowNum, j)).reduce(0,
					(j, k) -> j + k);
			d.set(rowNum, 0, -(PRrate + SatClkDrift - SatVelLOS));

		}
		SimpleMatrix H;
		SimpleMatrix unitVector = new SimpleMatrix(SVcount, 1);
		unitVector.fill(1);
		if (isStatic) {
			H = unitVector;
		} else {
			H = unitLOS.concatColumns(unitVector);
		}
		SimpleMatrix Ht = H.transpose();
		SimpleMatrix W = new SimpleMatrix(Weight);
		SimpleMatrix HtWHinv = (Ht.mult(W).mult(H)).invert();
		SimpleMatrix g = HtWHinv.mult(Ht).mult(W).mult(d);
		if (isStatic) {
			approxRcvrClkDrift = -g.get(0) / SpeedofLight;
		} else {
			estVel = Math
					.sqrt(IntStream.range(0, 3).mapToDouble(i -> Math.pow(g.get(i), 2)).reduce(0, (x, y) -> x + y));
			approxRcvrClkDrift = -g.get(3) / SpeedofLight;
		}

	}

	public double getRcvrClkDrift() {
		return approxRcvrClkDrift;
	}

	public double getEstVel() {
		return estVel;
	}

	public void setEstECEF(double[] estECEF) {
		this.estECEF = estECEF;
	}

	public void setEstVel(double estVel) {
		this.estVel = estVel;
	}

	public void setApproxRcvrClkOff(double approxRcvrClkOff) {
		this.approxRcvrClkOff = approxRcvrClkOff;
	}

	public void setApproxRcvrClkDrift(double approxRcvrClkDrift) {
		this.approxRcvrClkDrift = approxRcvrClkDrift;
	}

	public ArrayList<Satellite> getSV() {
		return SV;
	}

	public void setSV(ArrayList<Satellite> SV) {
		this.SV = SV;
	}

	public Calendar getTime() {
		return time;
	}

	public void setTime(Calendar time) {
		this.time = time;
	}

}
