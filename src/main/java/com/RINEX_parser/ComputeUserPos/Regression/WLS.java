package com.RINEX_parser.ComputeUserPos.Regression;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.orekit.models.earth.Geoid;

import com.RINEX_parser.ComputeUserPos.Regression.Models.LinearLeastSquare;
import com.RINEX_parser.fileParser.IONEX;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.Weight;

public class WLS extends LinearLeastSquare {

	public WLS(ArrayList<Satellite> SV, double[] PCO, IonoCoeff ionoCoeff, Calendar time,
			HashMap<Integer, HashMap<Integer, Double>> sbasIVD, IONEX ionex, Geoid geoid) {
		super(SV, PCO, ionoCoeff, time, sbasIVD, ionex, geoid);

		setWeight(SV, false);

	}

	public WLS(ArrayList<Satellite> SV, double[] PCO, IonoCoeff ionoCoeff, Calendar time, IONEX ionex, Geoid geoid) {
		this(SV, PCO, ionoCoeff, time, null, ionex, geoid);

	}

	@Override
	public double[] getEstECEF() {
		estimate(getPR());

		return super.getEstECEF();

	}

	public double[] getEstECEF(double[] PR) {
		estimate(PR);

		return super.getEstECEF();

	}

	public double[] getIonoCorrECEF() {

		estimate(getIonoCorrPR());

		return super.getEstECEF();
	}

	public double[] getTropoCorrECEF() {
		double[] ionoCorrECEF = getIonoCorrECEF();
		double[] ionoCorrLLH = ECEFtoLatLon.ecef2lla(ionoCorrECEF);
		estimate(getTropoCorrPR(ionoCorrLLH));
		return super.getEstECEF();
	}

	public void setWeight(ArrayList<Satellite> SV, boolean useAndroidParam) {

		super.setWeight(Weight.computeWeight(SV, useAndroidParam));
	}

}
