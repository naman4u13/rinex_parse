package com.RINEX_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.RINEX_parser.fileParser.Antenna;
import com.RINEX_parser.fileParser.Bias;
import com.RINEX_parser.fileParser.Clock;
import com.RINEX_parser.fileParser.Orbit;
import com.RINEX_parser.fileParser.SBAS;
import com.RINEX_parser.helper.ComputeAzmEle;
import com.RINEX_parser.helper.ComputeIonoCorr;
import com.RINEX_parser.helper.ComputeSatPos;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.IonoValue;
import com.RINEX_parser.models.NavigationMsg;
import com.RINEX_parser.models.Observable;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.SBAS.Correction;
import com.RINEX_parser.models.SBAS.LongTermCorr;
import com.RINEX_parser.utility.Closest;

public class SingleFreq {
	final static double SpeedofLight = 299792458;

	public static ArrayList<Satellite> process(ObservationMsg obsvMsg,
			HashMap<Integer, ArrayList<NavigationMsg>> NavMsgs, String obsvCode, boolean useIGS, boolean useSBAS,
			boolean doIonoPlot, boolean useBias, IonoCoeff ionoCoeff, Bias bias, Orbit orbit, Clock clock,
			Antenna antenna, long tRX, long weekNo, Calendar time, SBAS sbas, double[] userECEF, double[] userLatLon,
			HashMap<Integer, ArrayList<IonoValue>> ionoValueMap) {
		ArrayList<Observable> observables = obsvMsg.getObsvSat(obsvCode);
		ArrayList<Satellite> SV = new ArrayList<Satellite>();
		observables.removeAll(Collections.singleton(null));
		int satCount = observables.size();

		if (useIGS) {
			int polyOrder = 10;
			orbit.findPts(tRX, polyOrder);
			clock.findPts(tRX);
			for (int i = 0; i < satCount; i++) {
				Observable sat = observables.get(i);
				// PRN
				int SVID = sat.getSVID();
				double tSV = tRX - (sat.getPseudorange() / SpeedofLight);

				double satClkOff = clock.getBias(tSV, SVID, obsvCode);
				// GPS System transmission time
				double t = tSV - satClkOff;
				double[][] satPV = orbit.getPV(t, SVID, polyOrder);
				double[] satECEF = satPV[0];
				double[] satVel = satPV[1];

				double dotProd = 0;
				for (int j = 0; j < 3; j++) {
					dotProd += satECEF[j] * satVel[j];
				}
				double relativistic_error = -2 * (dotProd) / Math.pow(SpeedofLight, 2);
				// Correct sat clock offset for relativistic error and recompute the Sat coords
				satClkOff += relativistic_error;
				t = tSV - satClkOff;

				satECEF = antenna.getSatPC(SVID, obsvCode, tRX, weekNo, satECEF);
				Satellite _sat = new Satellite(sat, satECEF, satClkOff, t, tRX, satVel, 0.0, null, time);
				_sat.compECI();

				// double ISC = bias.getISC(obsvCode, SVID);
//				NavigationMsg NavMsg = NavMsgs.get(SVID).get(order[i]);
//				Object[] SatParams = ComputeSatPos.computeSatPos(NavMsg, tSV, tRX, null, 0);
				// double[] ECEF_SatClkOff = (double[]) SatParams[0];
//				double XYZdiff = Math
//						.sqrt(IntStream.range(0, 3).mapToDouble(j -> satECEF[j] - ECEF_SatClkOff[j])
//								.map(j -> j * j).reduce(0, (j, k) -> j + k));
//				double clkDiff = SpeedofLight
//						* ((satClkOff + relativistic_error) - (ECEF_SatClkOff[3] + NavMsg.getTGD()));
//			double TGDdiff = SpeedofLight * (temp[1] - (NavMsg.getTGD()));

				SV.add(_sat);

			}

		} else {
			// find out index of nav-msg inside the nav-msg list which is most suitable for
			// each obs-msg based on time
			int order[] = observables.stream().map(i -> NavMsgs.get(i.getSVID()))
					.map(i -> (ArrayList<Long>) i.stream().map(j -> j.getTOC()).collect(Collectors.toList()))
					.mapToInt(i -> Closest.findClosest(tRX, i)).toArray();

			HashMap<Integer, Correction> PRNmap = null;
			HashMap<Integer, HashMap<Integer, Double>> sbasIVD = null;
			if (useSBAS) {
				sbas.process(tRX);
				PRNmap = sbas.getPRNmap();
				if (sbas.isIonoEnabled()) {
					sbasIVD = sbas.getIonoVDelay();

					for (int lat : sbasIVD.keySet()) {
						for (int lon : sbasIVD.get(lat).keySet()) {
							double val = sbasIVD.get(lat).get(lon);
							// IPPdelay.add(new String[] { "" + lat, "" + lon, "" + tRX, "" + val });
						}
					}

				} else {
					System.out.println("NO SBAS Iono Corr");

				}

			}

			for (int i = 0; i < order.length; i++) {

				Observable sat = observables.get(i);
				// PRN
				int SVID = sat.getSVID();
				// SBAS params
				double PRC = 0;
				LongTermCorr ltc = null;
				// IGS .BSX file DCB
				double ISC = 0;

				NavigationMsg NavMsg = NavMsgs.get(SVID).get(order[i]);
				// Incase Msg1 or PRN mask hasn't been assigned PRNmap will be null
				if (useSBAS && PRNmap != null) {
					Correction corr = PRNmap.get(SVID);
					if (corr != null) {
						if (corr.getFC() != null) {
							PRC = corr.getFC().getPRC();
						}
						ltc = corr.getLTC().get(NavMsg.getIODE());
					}

				}
				if (useBias) {
					ISC = bias.getISC(obsvCode, SVID);

				}

				// IODEmap.computeIfAbsent(SVID, k -> new
				// HashSet<Integer>()).add(NavMsg.getIODE());
				sat.setPseudorange(sat.getPseudorange() + PRC);
				double tSV = tRX - (sat.getPseudorange() / SpeedofLight);

				Object[] SatParams = ComputeSatPos.computeSatPos(NavMsg, tSV, tRX, ltc, ISC);
				double[] ECEF_SatClkOff = (double[]) SatParams[0];
				double[] SatVel = (double[]) SatParams[1];
				// Note this Clock Drift is derived, it not what we get from Ephemeris
				double SatClkDrift = (double) SatParams[2];
				// GPS System time at time of transmission
				double t = (double) SatParams[3];
				// ECI coordinates
				double[] ECI = (double[]) SatParams[4];
				SV.add(new Satellite(sat, Arrays.copyOfRange(ECEF_SatClkOff, 0, 3), ECEF_SatClkOff[3], t, tRX, SatVel,
						SatClkDrift, ECI, time));
				if (doIonoPlot) {

					double[] AzmEle = ComputeAzmEle.computeAzmEle(userECEF, Arrays.copyOfRange(ECEF_SatClkOff, 0, 3));

					double ionoCorr = ComputeIonoCorr.computeIonoCorr(AzmEle[0], AzmEle[1], userLatLon[0],
							userLatLon[1], tRX, ionoCoeff);

					ionoValueMap.computeIfAbsent(SVID, k -> new ArrayList<IonoValue>())
							.add(new IonoValue(time.getTime(), ionoCorr, SVID));

				}

			}
		}
		return SV;
	}

}
