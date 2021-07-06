package com.RINEX_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import com.RINEX_parser.fileParser.Antenna;
import com.RINEX_parser.fileParser.Bias;
import com.RINEX_parser.fileParser.Clock;
import com.RINEX_parser.fileParser.Orbit;
import com.RINEX_parser.fileParser.SBAS;
import com.RINEX_parser.helper.ComputeEleAzm;
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
import com.RINEX_parser.utility.Vector;

public class SingleFreq {
	final static double SpeedofLight = 299792458;

	public static ArrayList<Satellite> process(ObservationMsg obsvMsg,
			HashMap<Integer, ArrayList<NavigationMsg>> NavMsgs, String obsvCode, boolean useIGS, boolean useSBAS,
			boolean doIonoPlot, boolean useBias, IonoCoeff ionoCoeff, Bias bias, Orbit orbit, Clock clock,
			Antenna antenna, double tRX, long weekNo, Calendar time, SBAS sbas, double[] userECEF, double[] userLatLon,
			HashMap<Integer, ArrayList<IonoValue>> ionoValueMap, boolean useCutOffAng, TopocentricFrame tpf,
			Frame frame) {
		ArrayList<Satellite> SV = new ArrayList<Satellite>();
		ArrayList<Observable> observables = obsvMsg.getObsvSat(obsvCode);
		if (observables == null) {
			return SV;
		}
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

				double relativistic_error = -2 * (Vector.dotProd(satECEF, satVel)) / Math.pow(SpeedofLight, 2);
				// Correct sat clock offset for relativistic error and recompute the Sat coords
				satClkOff += relativistic_error;
				t = tSV - satClkOff;

				double[] satPC_windup = antenna.getSatPC_windup(SVID, obsvCode, tRX, weekNo, satECEF, userECEF);
				IntStream.range(0, 3).forEach(j -> satECEF[j] = satPC_windup[j]);
				// fractional Wind up in cycles, will require further processing to correct for
				// full cycles and then multiply by wavelength
				double windup = satPC_windup[3];

				double[] EleAzm = ComputeEleAzm.computeEleAzm(userECEF, satECEF);
				Satellite _sat = new Satellite(sat, satECEF, satClkOff, t, tRX, satVel, 0.0, null, EleAzm, time);
				_sat.compECI();
				_sat.setPhaseWindUp(windup);

				SV.add(_sat);

			}

		} else {
			// find out index of nav-msg inside the nav-msg list which is most suitable for
			// each obs-msg based on time
			int order[] = observables.stream().map(i -> NavMsgs.get(i.getSVID()))
					.map(i -> (ArrayList<Double>) i.stream().map(j -> j.getTOC()).collect(Collectors.toList()))
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
				AbsoluteDate date = new AbsoluteDate(time.getTime(), TimeScalesFactory.getGPS());
//				double ele = tpf.getElevation(new Vector3D(Arrays.copyOfRange(ECEF_SatClkOff, 0, 3)), frame, date);
//				double az = tpf.getAzimuth(new Vector3D(Arrays.copyOfRange(ECEF_SatClkOff, 0, 3)), frame, date);

				double[] EleAzm = ComputeEleAzm.computeEleAzm(userECEF, Arrays.copyOfRange(ECEF_SatClkOff, 0, 3));
//				az %= 2 * Math.PI;
//				if (az > Math.PI) {
//					az += -2 * Math.PI;
//				} else if (az < -Math.PI) {
//					az += 2 * Math.PI;
//				}
				SV.add(new Satellite(sat, Arrays.copyOfRange(ECEF_SatClkOff, 0, 3), ECEF_SatClkOff[3], t, tRX, SatVel,
						SatClkDrift, ECI, EleAzm, time));
//				if (Math.abs(ele - EleAzm[0]) > 0.0001 || Math.abs(az - EleAzm[1]) > 0.0001) {
//					System.err.println("Elevation and Azimuth are wrongly estimated");
//				}
				if (doIonoPlot) {

					double freq = SV.get(0).getCarrier_frequency();

					double ionoCorr = ComputeIonoCorr.computeIonoCorr(EleAzm[0], EleAzm[1], userLatLon[0],
							userLatLon[1], tRX, ionoCoeff, freq, time);

					ionoValueMap.computeIfAbsent(SVID, k -> new ArrayList<IonoValue>())
							.add(new IonoValue(time.getTime(), ionoCorr, SVID));

				}

			}
		}

		if (useCutOffAng) {
			SV.removeIf(i -> i.getElevAzm()[0] < Math.toRadians(5));
		}

		return SV;
	}

}
