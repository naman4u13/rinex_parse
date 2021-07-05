package com.RINEX_parser.GoogleDecimeter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

import com.RINEX_parser.helper.ComputeEleAzm;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.Observable;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.GoogleDecimeter.Derived;

public class SingleFreq {

	public static ArrayList<Satellite> process(ObservationMsg obsvMsg, String obsvCode, IonoCoeff ionoCoeff, double tRX,
			double[] userECEF, double[] userLatLon, boolean useCutOffAng,
			HashMap<Double, HashMap<String, HashMap<Integer, Derived>>> derivedMap, Calendar time) {

		ArrayList<Satellite> SV = new ArrayList<Satellite>();
		ArrayList<Observable> observables = obsvMsg.getObsvSat(obsvCode);
		if (observables == null) {
			return SV;
		}
		observables.removeAll(Collections.singleton(null));
		int satCount = observables.size();
		for (int i = 0; i < satCount; i++) {
			Observable sat = observables.get(i);
			int svid = sat.getSVID();
			double key = Math.round(tRX * 1000) / 1000;
			Derived navData = derivedMap.get(key).get(obsvCode).get(svid);
			double[] satECEF = navData.getSatECEF();
			double[] EleAzm = ComputeEleAzm.computeEleAzm(userECEF, Arrays.copyOfRange(satECEF, 0, 3));
			double t = navData.gettSV() - navData.getSatClkBias();
			// SV.add(new Satellite(sat, satECEF, navData.getSatClkBias(), t, tRX, satVel,
			// satClkDrift, ECI, ElevAzm, time))

		}

		return null;

	}
}
