package com.RINEX_parser.ComputeUserPos.Regression.DualFreq;

import java.util.ArrayList;
import java.util.Calendar;

import org.orekit.models.earth.Geoid;

import com.RINEX_parser.ComputeUserPos.Regression.Models.DualFreq.LinearLeastSquare;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.Weight;

public class WLS extends LinearLeastSquare {

	public WLS(ArrayList<Satellite>[] SV, double[][] PCO, double[] refECEF, Calendar time, Geoid geoid) {
		super(SV, PCO, refECEF, time, geoid);
		// TODO Auto-generated constructor stub
		setWeight();
	}

	@Override
	public double[] getEstECEF() {
		estimate(getPR());

		return super.getEstECEF();

	}

	public double[] getTropoCorrECEF() {

		estimate(getTropoCorrPR());
		return super.getEstECEF();
	}

	public void setWeight() {

		super.setWeight(Weight.computeWeight(getSV()));
	}

}
