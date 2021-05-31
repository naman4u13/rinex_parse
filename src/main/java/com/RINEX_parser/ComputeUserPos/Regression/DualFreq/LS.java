package com.RINEX_parser.ComputeUserPos.Regression.DualFreq;

import java.util.ArrayList;
import java.util.Calendar;

import org.orekit.models.earth.Geoid;

import com.RINEX_parser.ComputeUserPos.Regression.Models.DualFreq.LinearLeastSquare;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.Weight;

public class LS extends LinearLeastSquare {

	public LS(ArrayList<Satellite>[] SV, double[][] PCO, double[] refECEF, Calendar time) {
		super(SV, PCO, refECEF, time);
		int SVcount = SV[0].size();
		setWeight(Weight.computeIdentityMat(2 * SVcount));

		// TODO Auto-generated constructor stub
	}

	@Override
	public double[] getEstECEF() {
		estimate(getPR());
		return super.getEstECEF();

	}

	public double[] getTropoCorrECEF(Geoid geoid) {

		estimate(getTropoCorrPR(geoid));
		return super.getEstECEF();
	}
}
