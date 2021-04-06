package com.RINEX_parser.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.stream.IntStream;

import com.RINEX_parser.ComputeUserPos.Regression.LS;
import com.RINEX_parser.ComputeUserPos.Regression.WLS;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;

public class SatUtil {

	static final double SpeedofLight = 299792458;
	private double[] approxECEF;
	private double rcvrClkOff;

	public SatUtil(ArrayList<Satellite> SV, Calendar time) {

		LS ls = new LS(SV, time);
		ls.estimate(ls.getPR());
		approxECEF = ls.getEstECEF();
		rcvrClkOff = ls.getRcvrClkOff();

	}

	public SatUtil(double[] trueECEF) {
		approxECEF = trueECEF;
	}

	public double[][] getWeightMat(ArrayList<Satellite> SV, Calendar time) {
		WLS wls = new WLS(SV, time);
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

	public double[] getIonoCorrPR(ArrayList<Satellite> SV, IonoCoeff ionoCoeff, Calendar time) {
		LS ls = new LS(SV, ionoCoeff, time);
		return ls.getIonoCorrPR();
	}

	public double[] getUserECEF() {
		return new double[] { approxECEF[0], approxECEF[1], approxECEF[2], rcvrClkOff };
	}
}
