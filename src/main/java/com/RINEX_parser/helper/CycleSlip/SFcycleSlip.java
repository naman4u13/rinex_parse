package com.RINEX_parser.helper.CycleSlip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.CycleSlip.LinearCombo;
import com.RINEX_parser.models.CycleSlip.SFfilter;

public class SFcycleSlip {

	public static HashMap<String, int[]> process(ArrayList<ArrayList<Satellite>> SVlist, int samplingRate) {
		// maximum period of time allowed without declaring cycle slip
		double maxDGper = 40;

		// Min arc length for SF CSD
		int minAL_SF = 3;
		int nT = 5;
		double threshMax = 20;
		HashMap<String, SFfilter> sfMap = new HashMap<String, SFfilter>();
		HashMap<String, int[]> res = new HashMap<String, int[]>();
		for (int i = 0; i < SVlist.size(); i++) {

			ArrayList<Satellite> SV = SVlist.get(i);
			double t = SV.get(0).gettRX();
			for (int j = 0; j < SV.size(); j++) {
				Satellite sat = SV.get(j);
				String SVID = sat.getSSI() + "" + sat.getSVID();
				// phase code diff
				double d = (sat.getCarrier_frequency() * sat.getCarrier_wavelength()) - sat.getPseudorange();
				SFfilter sfObj = sfMap.getOrDefault(SVID, null);
				boolean isSlip = true;
				if (sfObj == null) {
					sfObj = new SFfilter(samplingRate);
				} else {
					List<LinearCombo> dList = sfObj.getdList();
					double timeGap = t - dList.get(dList.size() - 1).t();
					if (timeGap > maxDGper) {
						sfObj = new SFfilter(samplingRate);

					} else if (dList.size() >= minAL_SF) {

						double calcThresh = nT * Math.sqrt(sfObj.getSigmaSq());
						double thresh = Math.min(calcThresh, threshMax);
						double diffAbs = Math.abs(d - sfObj.getMean());

						isSlip = diffAbs > calcThresh;
						if (isSlip) {
							sfObj = sfObj.reset(samplingRate, minAL_SF);
						}
					}

				}
				SV.get(j).setLocked(!isSlip);
				sfObj.update(d, t);
				sfMap.put(SVID, sfObj);
				res.computeIfAbsent(SVID, k -> new int[SVlist.size()])[i] = isSlip ? 1 : 2;
			}
		}
		return res;
	}
}
