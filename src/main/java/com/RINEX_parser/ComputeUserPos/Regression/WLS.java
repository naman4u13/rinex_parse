package com.RINEX_parser.ComputeUserPos.Regression;

import java.util.ArrayList;

import com.RINEX_parser.ComputeUserPos.Regression.Models.LinearLeastSquare;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.Weight;

public class WLS extends LinearLeastSquare {

	public WLS(ArrayList<Satellite> SV, IonoCoeff ionoCoeff) {
		super(SV, ionoCoeff);

		setWeight(SV);

	}

	public WLS(ArrayList<Satellite> SV) {
		super(SV);

		setWeight(SV);
	}

	@Override
	public double[] getEstECEF() {
		estimate(getPR());
		System.out.println("\nPGDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();

	}

	public double[] getIonoCorrECEF() {

		estimate(getIonoCorrPR());
		System.out.println("Iono WPDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
//		computeRcvrInfo(true);
//		System.out.println("Rcvr Velocity - " + getEstVel() + "  Rcvr Clk Drift - " + getRcvrClkDrift());
		return super.getEstECEF();
	}

	public void setWeight(ArrayList<Satellite> SV) {

		super.setWeight(Weight.computeWeight(SV));
	}

}
