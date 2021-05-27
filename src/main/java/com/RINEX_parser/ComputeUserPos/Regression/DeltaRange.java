package com.RINEX_parser.ComputeUserPos.Regression;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import com.RINEX_parser.ComputeUserPos.Regression.Models.DeltaRangeLLS;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.Weight;

public class DeltaRange extends DeltaRangeLLS {

	public DeltaRange(ArrayList<Satellite> SV_t, ArrayList<Satellite> SV_tmin1, double[] PCO, Calendar time) {
		// Reduntant as correct SV will be set below
		super(SV_t, PCO, time);
		// Correct SV will now be set
		checkCompatibleSV(SV_t, SV_tmin1);

	}

	@Override
	public double[] getEstECEF() {
		estimate();
		System.out.println("\nPDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();

	}

	public double[] getEstECEF(IonoCoeff ionoCoeff) {
		WLS wls = new WLS(getSV(), getPCO(), ionoCoeff, getTime());
		double[] userECEF = wls.getIonoCorrECEF();
		estimate(userECEF);
		System.out.println("\nPDOP - " + Math.sqrt(getCovdX().extractMatrix(0, 3, 0, 3).trace()));
		return super.getEstECEF();

	}

	// find out common satellite present on both epochs - t and t-1, and arrange
	// them in same order
	public void checkCompatibleSV(ArrayList<Satellite> SV_t, ArrayList<Satellite> SV_tmin1) {
		ArrayList<Satellite> new_SV_t = new ArrayList<Satellite>();
		ArrayList<Satellite> new_SV_tmin1 = new ArrayList<Satellite>();
		HashMap<Integer, Satellite> map = new HashMap<Integer, Satellite>();
		HashSet<Integer> set = new HashSet<Integer>();
		SV_t.stream().forEach(i -> {
			map.put(i.getSVID(), i);
			set.add(i.getSVID());
		});

		for (Satellite sat : SV_tmin1) {
			int svid = sat.getSVID();
			if (set.contains(svid)) {
				new_SV_t.add(map.get(svid));
				new_SV_tmin1.add(sat);
			}
		}
		setSVs(new_SV_t, new_SV_tmin1);
		setWeight(Weight.computeIdentityMat(new_SV_t.size()));

	}

}
