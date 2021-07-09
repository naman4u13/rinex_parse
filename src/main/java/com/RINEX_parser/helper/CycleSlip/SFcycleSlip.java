package com.RINEX_parser.helper.CycleSlip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.CycleSlip.LinearCombo;
import com.RINEX_parser.models.CycleSlip.MWfilter;
import com.RINEX_parser.models.CycleSlip.SFfilter;

public class SFcycleSlip {

	public static void process(ArrayList<ArrayList<Satellite>> SVlist, int samplingRate) {
		HashMap<String, SFfilter> sfMap = new HashMap<String, SFfilter>();
		for (int i = 0; i < SVlist.size(); i++) {
			ArrayList<Satellite> SV = SVlist.get(i);
			for (int j = 0; j < SV.size(); j++) {
				Satellite sat = SV.get(j);

				String SVID = "" + sat.getSSI() + sat.getSVID();
				double d = (sat.getCarrier_frequency() * sat.getCarrier_wavelength()) - sat.getPseudorange();
				SFfilter sfObj = sfMap.getOrDefault(SVID, null);
				if (sfObj == null) {
					sfObj = new SFfilter(samplingRate);
				} else {
					List<LinearCombo> dList = sfObj.getdList();
					if (t - mwList.get(mwList.size() - 1).t() > maxDGper) {
						mwObj = new MWfilter(samplingRate);

					} else {
						if (mwList.size() >= minAL_MW) {

							double d = mw - mwObj.getMean();
							double d300 = mw - mwObj.getMean300();
							double sigma = Math.sqrt(mwObj.getSigmaSq());
							double th = kFact * sigma;
							isMWslip = (Math.abs(d) > th) && (sigma <= lamW) && (Math.abs(d300) > lamW);
							if (isMWslip) {
								mwObj = mwObj.reset(samplingRate);
							}
						}
					}

				}

				boolean isSlip = isGFslip || isMWslip;
				SV[0].get(j).setLocked(!isSlip);
				SV[1].get(j).setLocked(!isSlip);

			}
		}
	}
}
