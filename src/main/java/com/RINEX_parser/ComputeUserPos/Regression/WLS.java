package com.RINEX_parser.ComputeUserPos.Regression;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.orekit.models.earth.Geoid;

import com.RINEX_parser.ComputeUserPos.Regression.Models.LinearLeastSquare;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.Weight;

public class WLS extends LinearLeastSquare {

	public WLS(ArrayList<Satellite> SV, IonoCoeff ionoCoeff, Calendar time) {
		super(SV, ionoCoeff, time);

		setWeight(SV);

	}

	public WLS(ArrayList<Satellite> SV, Calendar time) {
		super(SV, time);

		setWeight(SV);
	}

	@Override
	public double[] getEstECEF() {
		estimate(getPR());

		return super.getEstECEF();

	}

	public double[] getIonoCorrECEF(HashMap<Integer, HashMap<Integer, Double>> sbasIVD) {

		estimate(getIonoCorrPR(sbasIVD));

		return super.getEstECEF();
	}

	public double[] getIonoCorrECEF() {

		estimate(getIonoCorrPR());

		return super.getEstECEF();
	}

	public double[] getTropoCorrECEF(Geoid geoid) {
		double[] ionoCorrECEF = getIonoCorrECEF();
		double[] ionoCorrLLH = ECEFtoLatLon.ecef2lla(ionoCorrECEF);
		estimate(getTropoCorrPR(ionoCorrLLH, geoid));
		return super.getEstECEF();
	}

	public double[] getTropoCorrECEF(Geoid geoid, HashMap<Integer, HashMap<Integer, Double>> sbasIVD) {
		double[] ionoCorrECEF = getIonoCorrECEF(sbasIVD);
		double[] ionoCorrLLH = ECEFtoLatLon.ecef2lla(ionoCorrECEF);
		estimate(getTropoCorrPR(ionoCorrLLH, geoid));
		return super.getEstECEF();
	}

	public void setWeight(ArrayList<Satellite> SV) {

		super.setWeight(Weight.computeWeight(this));
	}

}
