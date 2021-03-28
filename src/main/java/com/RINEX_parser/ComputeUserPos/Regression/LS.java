package com.RINEX_parser.ComputeUserPos.Regression;

import java.util.ArrayList;

import com.RINEX_parser.ComputeUserPos.Regression.Models.LinearLeastSquare;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.Weight;

public class LS extends LinearLeastSquare {

	public LS(ArrayList<Satellite> SV, IonoCoeff ionoCoeff) {
		super(SV, ionoCoeff);
		int SVcount = SV.size();
		setWeight(Weight.computeIdentityMat(SVcount));

	}

	public LS(ArrayList<Satellite> SV) {
		super(SV);
		int SVcount = SV.size();
		setWeight(Weight.computeIdentityMat(SVcount));
	}

	@Override
	public double[] getEstECEF() {
		estimate(getPR());
		System.out.println("\nPDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();

	}

	public double[] getIonoCorrECEF() {

		estimate(getIonoCorrPR());
		System.out.println("Iono PDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();
	}

}
