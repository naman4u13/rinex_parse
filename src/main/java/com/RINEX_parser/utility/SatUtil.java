package com.RINEX_parser.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.stream.IntStream;

import org.orekit.models.earth.Geoid;

import com.RINEX_parser.ComputeUserPos.Regression.Models.LinearLeastSquare;
import com.RINEX_parser.fileParser.IONEX;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;

public class SatUtil {
	private static final double SpeedofLight = 299792458;

	public static double[][] getUnitLOS(ArrayList<Satellite> SV, double[] userXYZ) {
		int SVcount = SV.size();
		double[][] unitLOS = new double[SVcount][3];
		for (int k = 0; k < SVcount; k++) {
			Satellite sat = SV.get(k);
			// Line of Sight vector
			double[] LOS = IntStream.range(0, 3).mapToDouble(i -> sat.getECI()[i] - userXYZ[i]).toArray();
			double GeometricRange = Math.sqrt(Arrays.stream(LOS).map(i -> i * i).reduce(0.0, (i, j) -> i + j));
			// Converting LOS to unit vector
			unitLOS[k] = Arrays.stream(LOS).map(i -> i / GeometricRange).toArray();
		}
		return unitLOS;

	}

	public static double[][] getUnitLOS(double[][] satXYZ, double[] userXYZ) {
		int SVcount = satXYZ.length;
		double[][] unitLOS = new double[SVcount][3];
		for (int k = 0; k < SVcount; k++) {
			final int _k = k;
			// Line of Sight vector
			double[] LOS = IntStream.range(0, 3).mapToDouble(i -> satXYZ[_k][i] - userXYZ[i]).toArray();
			double GeometricRange = Math.sqrt(Arrays.stream(LOS).map(i -> i * i).reduce(0.0, (i, j) -> i + j));
			// Converting LOS to unit vector
			unitLOS[k] = Arrays.stream(LOS).map(i -> i / GeometricRange).toArray();
		}
		return unitLOS;

	}

	public static double[] getUnitLOS(double[] satXYZ, double[] userXYZ) {

		double[] unitLOS = new double[3];

		// Line of Sight vector
		double[] LOS = IntStream.range(0, 3).mapToDouble(i -> satXYZ[i] - userXYZ[i]).toArray();
		double GeometricRange = Math.sqrt(Arrays.stream(LOS).map(i -> i * i).reduce(0.0, (i, j) -> i + j));
		// Converting LOS to unit vector
		unitLOS = Arrays.stream(LOS).map(i -> i / GeometricRange).toArray();

		return unitLOS;

	}

	private double[] sPCO;
	private double[][] dPCO;
	private IonoCoeff ionoCoeff;
	private HashMap<Integer, HashMap<Integer, Double>> sbasIVD;
	private IONEX ionex;
	private Geoid geoid;

	public SatUtil(double[] PCO, IonoCoeff ionoCoeff, HashMap<Integer, HashMap<Integer, Double>> sbasIVD, IONEX ionex,
			Geoid geoid) {
		this.geoid = geoid;
		this.sPCO = PCO;
		this.ionex = ionex;
		this.ionoCoeff = ionoCoeff;
		this.sbasIVD = sbasIVD;
	}

	public SatUtil(double[][] PCO, Geoid geoid) {
		this.dPCO = PCO;
		this.geoid = geoid;
	}

	public double[] getTropoCorrPR(ArrayList<Satellite> SV, Calendar time, double[] refLatLon) {
		LinearLeastSquare lls = new LinearLeastSquare(SV, sPCO, ionoCoeff, time, sbasIVD, ionex, geoid);
		return lls.getTropoCorrPR(refLatLon);
	}

	public double[][] getTropoCorrPR(ArrayList<Satellite>[] SV, Calendar time, double[] refECEF) {
		com.RINEX_parser.ComputeUserPos.Regression.Models.DualFreq.LinearLeastSquare lls = new com.RINEX_parser.ComputeUserPos.Regression.Models.DualFreq.LinearLeastSquare(
				SV, dPCO, refECEF, time, geoid);

		return lls.getTropoCorrPR();
	}
}
