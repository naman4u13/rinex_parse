package com.RINEX_parser.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import com.RINEX_parser.ComputeUserPos.Regression.LS;
import com.RINEX_parser.ComputeUserPos.Regression.WLS;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;

public class SatUtil {

	static final double SpeedofLight = 299792458;
	private double[] approxECEF;

	public SatUtil(ArrayList<Satellite> SV) {

		LS ls = new LS(SV);
		ls.estimate(ls.getPR());
		approxECEF = ls.getEstECEF();

	}

	public SatUtil(double[] trueECEF) {
		approxECEF = trueECEF;
	}

	public double[][] getWeightMat(ArrayList<Satellite> SV) {
		WLS wls = new WLS(SV);
		return wls.getWeight();
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

	public double[] getIonoCorrPR(ArrayList<Satellite> SV, IonoCoeff ionoCoeff) {
		LS ls = new LS(SV, ionoCoeff);
		return ls.getIonoCorrPR();
	}

	public double[] getUserECEF() {
		return approxECEF;
	}
}
