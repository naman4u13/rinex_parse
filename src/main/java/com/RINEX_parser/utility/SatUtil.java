package com.RINEX_parser.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.RINEX_parser.ComputeUserPos.LeastSquare;
import com.RINEX_parser.helper.ComputeAzmEle;
import com.RINEX_parser.models.Satellite;

public class SatUtil {

	static final double SpeedofLight = 299792458;
	private double[] approxECEF;

	public SatUtil(ArrayList<Satellite> SV) {
		// Removed satellite clock offset error from pseudorange
		double[] PR = SV.stream().mapToDouble(x -> x.getPseudorange() + (SpeedofLight * x.getSatClkOff())).toArray();
		approxECEF = LeastSquare.trilateration(SV, PR);
	}

	public double[][] getWeightMat(ArrayList<Satellite> SV) {
		int SVcount = SV.size();
		ArrayList<double[]> AzmEle = (ArrayList<double[]>) SV.stream()
				.map(i -> ComputeAzmEle.computeAzmEle(approxECEF, i.getECEF())).collect(Collectors.toList());
		// Computing weight matrix
		double[][] Weight = new double[SVcount][SVcount];
		IntStream.range(0, SVcount)
				.forEach(i -> Weight[i][i] = 1 / computeCoVariance(SV.get(i).getCNo(), AzmEle.get(i)[0]));
		return Weight;
	}

	// Sarab Tay and Juliette Marais -
	// https://www.researchgate.net/publication/260200581_Weighting_models_for_GPS_Pseudorange_observations_for_land_transportation_in_urban_canyons
	// Hybrid WLS estimator combines both Carrier to Noise ratio and Elevation angle
	// info to compute weighing matrix, Intial results show its the superior
	// estimator in comparison to standalone estimator based on CNo or Elev angle.
	public double computeCoVariance(double CNo, double ElevAng) {
		double var = Math.pow(10, -(CNo / 10)) / Math.pow(Math.sin(ElevAng), 2);
		return var;
	}

	public double[][] getUnitLOS(ArrayList<Satellite> SV) {
		int SVcount = SV.size();
		double[][] unitLOS = new double[SVcount][4];
		for (int k = 0; k < SVcount; k++) {
			Satellite sat = SV.get(k);
			// Line of Sight vector
			double[] LOS = IntStream.range(0, 3).mapToDouble(i -> sat.getECEF()[i] - approxECEF[i]).toArray();
			double GeometricRange = Math.sqrt(Arrays.stream(LOS).map(i -> i * i).reduce(0.0, (i, j) -> i + j));
			// Converting LOS to unit vector
			unitLOS[k] = Arrays.stream(LOS).map(i -> i / GeometricRange).toArray();
		}
		return unitLOS;

	}

	public double[] getUserECEF() {
		return approxECEF;
	}
}
