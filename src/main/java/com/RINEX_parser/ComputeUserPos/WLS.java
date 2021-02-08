package com.RINEX_parser.ComputeUserPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;

import com.RINEX_parser.helper.ComputeAzmEle;
import com.RINEX_parser.helper.ComputeIonoCorr;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.ECEFtoLatLon;

public class WLS {
	static final double SpeedofLight = 299792458;

	public static ArrayList<Object> compute(ArrayList<Satellite> SV, IonoCoeff ionoCoeff, double[] userECEF) {

		// Removed satellite clock offset error from pseudorange
		double[] PR = SV.stream().mapToDouble(x -> x.getPseudorange() + (SpeedofLight * x.getSatClkOff())).toArray();
		double[] approxECEF = LeastSquare.trilateration(SV, PR);
		double[] approxLatLon = ECEFtoLatLon.ecef2lla(approxECEF);
		double[] userLatLon = ECEFtoLatLon.ecef2lla(userECEF);
		int SVcount = SV.size();
		// After we get the user ECEF, we will be able to calculate Azm&Ele angle which
		// will in
		// turn help us to calculate and remove iono error
		ArrayList<double[]> AzmEle = (ArrayList<double[]>) SV.stream()
				.map(i -> ComputeAzmEle.computeAzmEle(approxECEF, i.getECEF())).collect(Collectors.toList());

		ArrayList<double[]> AzmEle_deg = (ArrayList<double[]>) AzmEle.stream()
				.map(x -> new double[] { Math.toDegrees(x[0]), Math.toDegrees(x[1]) }).collect(Collectors.toList());
		double[] ionoCorr = IntStream
				.range(0, SV.size()).mapToDouble(x -> ComputeIonoCorr.computeIonoCorr(AzmEle.get(x)[0],
						AzmEle.get(x)[1], approxLatLon[0], approxLatLon[1], (long) SV.get(x).gettSV(), ionoCoeff))
				.toArray();
		System.out.println("IONO corrections");
		IntStream.range(0, ionoCorr.length)
				.forEach(i -> System.out.print("GPS" + SV.get(i).getSVID() + " - " + ionoCorr[i] + " "));

		System.out.println("");
		double[] ionoCorrPR = IntStream.range(0, PR.length).mapToDouble(x -> PR[x] - ionoCorr[x]).toArray();

		// Computing weight matrix
		double[][] Weight = new double[SVcount][SVcount];
		IntStream.range(0, SVcount)
				.forEach(i -> Weight[i][i] = 1 / computeCoVariance(SV.get(i).getCNo(), AzmEle.get(i)[0]));

		double[] ionoCorrECEF = trilateration(SV, ionoCorrPR, Weight);

		double[] approxWLS_ECEF = trilateration(SV, PR, Weight);

		// SV.stream().forEach(i -> computeRcvrClkDrift(i, ionoCorrECEF));
		double rcvrClkDrift = computeRcvrClkDriftStatic(SV, userECEF, Weight);
		return new ArrayList<Object>(Arrays.asList(approxWLS_ECEF, ionoCorrECEF, rcvrClkDrift));

	}

	public static double[] trilateration(ArrayList<Satellite> SV, double[] PR, double[][] Weight) {
		int SVcount = SV.size();
		if (SVcount >= 4) {
			double[] approxECEF = new double[] { 0, 0, 0 };
			double approxUserClkOff = 0;
			SimpleMatrix HtHinv = null;
			for (int i = 0; i < 100; i++) {
				double[][] deltaPR = new double[SVcount][1];
				// approx Geometric range or true range
				double[] approxGR = new double[SVcount];
				double[][] coeffA = new double[SVcount][4];
				for (int j = 0; j < SVcount; j++) {

					double[] satECEF = SV.get(j).getECEF();

					double ApproxGR = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> satECEF[x] - approxECEF[x])
							.map(x -> Math.pow(x, 2)).reduce((x, y) -> x + y).getAsDouble());
					approxGR[j] = ApproxGR;

					double approxPR = approxGR[j] + (SpeedofLight * approxUserClkOff);
					deltaPR[j][0] = approxPR - PR[j];
					int index = j;
					IntStream.range(0, 3).forEach(x -> coeffA[index][x] = (satECEF[x] - approxECEF[x]) / ApproxGR);
					coeffA[j][3] = 1;
				}
				SimpleMatrix H = new SimpleMatrix(coeffA);
				SimpleMatrix Ht = H.transpose();
				SimpleMatrix W = new SimpleMatrix(Weight);
				HtHinv = (Ht.mult(W).mult(H)).invert();
				SimpleMatrix DeltaPR = new SimpleMatrix(deltaPR);
				SimpleMatrix DeltaX = HtHinv.mult(Ht).mult(W).mult(DeltaPR);
				IntStream.range(0, 3).forEach(x -> approxECEF[x] = approxECEF[x] + DeltaX.get(x, 0));
				approxUserClkOff += (-DeltaX.get(3, 0)) / SpeedofLight;

			}
			if (HtHinv != null) {
				SimpleMatrix _HtHinv = HtHinv;
				double GDOP = Math
						.sqrt(IntStream.range(0, 3).mapToDouble(x -> _HtHinv.get(x, x)).reduce(0, (a, b) -> a + b));
				System.out.println("GDOP - " + GDOP);
			}

			return new double[] { approxECEF[0], approxECEF[1], approxECEF[2], approxUserClkOff };

		}
		System.out.println("Satellite count is less than 4, can't compute user position");
		return new double[] { -999 };
	}

	// Sarab Tay and Juliette Marais -
	// https://www.researchgate.net/publication/260200581_Weighting_models_for_GPS_Pseudorange_observations_for_land_transportation_in_urban_canyons
	// Hybrid WLS estimator combines both Carrier to Noise ratio and Elevation angle
	// info to compute weighing matrix, Intial results show its the superior
	// estimator in comparison to standalone estimator based on CNo or Elev angle.
	public static double computeCoVariance(double CNo, double ElevAng) {
		double var = Math.pow(10, -(CNo / 10)) / Math.pow(Math.sin(ElevAng), 2);
		return var;
	}

	public static double[][] normalize(double[][] W) {
		double sum = 0;
		int n = W.length;
		for (int i = 0; i < W.length; i++) {
			sum += W[i][i];
		}
		final double fSum = sum;
		IntStream.range(0, n).forEach(i -> W[i][i] = W[i][i] / fSum);
		return W;
	}

	public static double computeRcvrClkDriftStatic(ArrayList<Satellite> SV, double[] userECEF, double[][] Weight) {

		int n = SV.size();

		double[][] D = new double[n][1];
		double[][] h = new double[n][1];
		for (int x = 0; x < n; x++) {
			Satellite sat = SV.get(x);
			// Line of Sight vector
			double[] LOS = IntStream.range(0, 3).mapToDouble(i -> sat.getECEF()[i] - userECEF[i]).toArray();
			double GeometricRange = Math.sqrt(Arrays.stream(LOS).map(i -> i * i).reduce(0.0, (i, j) -> i + j));
			// Converting LOS to unit vector
			double[] LOSunit = Arrays.stream(LOS).map(i -> i / GeometricRange).toArray();
			double RangeRate = IntStream.range(0, 3).mapToDouble(i -> sat.getSatVel()[i] * LOSunit[i]).reduce(0.0,
					(i, j) -> i + j);
			D[x][0] = -(sat.getPseudoRangeRate() - RangeRate + (sat.getSatClkDrift() * SpeedofLight));
			h[x][0] = 1;

		}
		SimpleMatrix d = new SimpleMatrix(D);
		SimpleMatrix H = new SimpleMatrix(h);
		SimpleMatrix Ht = H.transpose();
		SimpleMatrix HtHinv = (Ht.mult(H)).invert();
		SimpleMatrix LS_g = HtHinv.mult(Ht).mult(d);
		SimpleMatrix W = new SimpleMatrix(Weight);
		HtHinv = (Ht.mult(W).mult(H)).invert();
		SimpleMatrix WLS_g = HtHinv.mult(Ht).mult(W).mult(d);
		double ls_g = -LS_g.get(0) / SpeedofLight;

		double wls_g = -WLS_g.get(0) / SpeedofLight;

		System.out.println("  LS RcvrClkDrift  " + ls_g + "  WLS RcvrClkDrift  " + wls_g);
		return wls_g;
	}

	public static double computeRcvrClkDriftDynamic(ArrayList<Satellite> SV, double[] userECEF, double[][] Weight) {

		int n = SV.size();
		double[][] LOSunit = new double[n][4];
		double[][] D = new double[n][1];
		for (int x = 0; x < n; x++) {
			Satellite sat = SV.get(x);
			// Line of Sight vector
			double[] LOS = IntStream.range(0, 3).mapToDouble(i -> sat.getECEF()[i] - userECEF[i]).toArray();
			double GeometricRange = Math.sqrt(Arrays.stream(LOS).map(i -> i * i).reduce(0.0, (i, j) -> i + j));
			// Converting LOS to unit vector
			final int index = x;
			IntStream.range(0, 3).forEach(i -> LOSunit[index][i] = LOS[i] / GeometricRange);
			LOSunit[x][3] = 1;
			double RangeRate = IntStream.range(0, 3).mapToDouble(i -> sat.getSatVel()[i] * LOSunit[index][i])
					.reduce(0.0, (i, j) -> i + j);
			D[x][0] = -(sat.getPseudoRangeRate() - RangeRate + (sat.getSatClkDrift() * SpeedofLight));

		}
		SimpleMatrix d = new SimpleMatrix(D);
		SimpleMatrix H = new SimpleMatrix(LOSunit);
		SimpleMatrix Ht = H.transpose();
		SimpleMatrix HtHinv = (Ht.mult(H)).invert();
		SimpleMatrix LS_g = HtHinv.mult(Ht).mult(d);
		SimpleMatrix W = new SimpleMatrix(Weight);
		HtHinv = (Ht.mult(W).mult(H)).invert();
		SimpleMatrix WLS_g = HtHinv.mult(Ht).mult(W).mult(d);
		double[] ls_g = IntStream.range(0, 4).mapToDouble(i -> LS_g.get(i)).toArray();
		ls_g[3] = -ls_g[3] / SpeedofLight;
		double ls_rv = Math
				.sqrt(IntStream.range(0, 3).mapToDouble(i -> ls_g[i] * ls_g[i]).reduce(0.0, (k, l) -> k + l));
		double[] wls_g = IntStream.range(0, 4).mapToDouble(i -> WLS_g.get(i)).toArray();
		wls_g[3] = -wls_g[3] / SpeedofLight;
		double wls_rv = Math
				.sqrt(IntStream.range(0, 3).mapToDouble(i -> wls_g[i] * wls_g[i]).reduce(0.0, (k, l) -> k + l));
		System.out.println(" LS RcvrClkDrift " + Arrays.toString(ls_g) + "   " + ls_rv + " WLS RcvrClkDrift "
				+ Arrays.toString(wls_g) + "   " + wls_rv);
		return wls_g[3];

	}

}
