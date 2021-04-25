package com.RINEX_parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jfree.ui.RefineryUtilities;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.models.earth.Geoid;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.utils.IERSConventions;

import com.RINEX_parser.ComputeUserPos.KalmanFilter.StaticEKF;
import com.RINEX_parser.ComputeUserPos.Regression.DeltaRange;
import com.RINEX_parser.ComputeUserPos.Regression.Doppler;
import com.RINEX_parser.ComputeUserPos.Regression.LS;
import com.RINEX_parser.ComputeUserPos.Regression.WLS;
import com.RINEX_parser.fileParser.NavigationRNX;
import com.RINEX_parser.fileParser.ObservationRNX;
import com.RINEX_parser.fileParser.SBAS;
import com.RINEX_parser.helper.ComputeAzmEle;
import com.RINEX_parser.helper.ComputeIonoCorr;
import com.RINEX_parser.helper.ComputeSatPos;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.IonoValue;
import com.RINEX_parser.models.NavigationMsg;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.SatelliteModel;
import com.RINEX_parser.models.TimeCorrection;
import com.RINEX_parser.models.SBAS.Correction;
import com.RINEX_parser.models.SBAS.LongTermCorr;
import com.RINEX_parser.utility.Closest;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.GraphPlotter;
import com.RINEX_parser.utility.IGPgrid;
import com.RINEX_parser.utility.LatLonDiff;
import com.RINEX_parser.utility.Time;

public class MainApp {

	public static void main(String[] args) {

		Instant start = Instant.now();
		posEstimate(false, false, true, true, true, 2);
		Instant end = Instant.now();
		System.out.println("EXECUTION TIME -  " + Duration.between(start, end));

	}

	public static void posEstimate(boolean doWeightPlot, boolean doIonoPlot, boolean doPosErrPlot, boolean useSNX,
			boolean useSBAS, int estimatorType) {
		Geoid geoid = buildGeoid();
		HashMap<Integer, ArrayList<IonoValue>> ionoValueMap = new HashMap<Integer, ArrayList<IonoValue>>();
		double SpeedofLight = 299792458;

		String nav_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\BRDC00IGS_R_20201000000_01D_MN.rnx\\BRDC00IGS_R_20201000000_01D_MN.rnx";

		String obs_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\MADR00ESP_R_20201001000_01H_30S_MO.crx\\MADR00ESP_R_20201001000_01H_30S_MO.rnx";

		String sbas_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\EGNOS_2020_100\\136\\h_09_10.ems";

		Map<String, Object> NavMsgComp = NavigationRNX.rinex_nav_process(nav_path);
		SBAS sbas = null;
		if (useSBAS) {
			int[][][] IGP = IGPgrid.readCSV();
			sbas = new SBAS(sbas_path, IGP);
		}
		HashMap<Integer, HashSet<Integer>> IODEmap = new HashMap<Integer, HashSet<Integer>>();
		String path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\MADR_SBAS136_RMS";
		File output = new File(path + ".txt");
		PrintStream stream;

		try {
			stream = new PrintStream(output);
			System.setOut(stream);
		} catch (FileNotFoundException e) { // TODO Auto-generated catch block
			e.printStackTrace();
		}

		@SuppressWarnings("unchecked")
		HashMap<Integer, ArrayList<NavigationMsg>> NavMsgs = (HashMap<Integer, ArrayList<NavigationMsg>>) NavMsgComp
				.get("NavMsgs");
		IonoCoeff ionoCoeff = (IonoCoeff) NavMsgComp.get("ionoCoeff");
		TimeCorrection timeCorr = (TimeCorrection) NavMsgComp.get("timeCorr");

		ArrayList<ObservationMsg> ObsvMsgs = ObservationRNX.rinex_obsv_process(obs_path, useSNX);

		HashMap<String, ArrayList<double[]>> ErrMap = new HashMap<String, ArrayList<double[]>>();
		HashMap<String, ArrayList<Double>> RcvrClkMap = new HashMap<String, ArrayList<Double>>();
		HashMap<String, ArrayList<Double>> WeightMap = new HashMap<String, ArrayList<Double>>();
		ArrayList<Calendar> timeList = new ArrayList<Calendar>();
		ArrayList<ArrayList<Satellite>> SVlist = new ArrayList<ArrayList<Satellite>>();
		double[] userECEF = null;
		for (ObservationMsg obsvMsg : ObsvMsgs) {

			long tRX = obsvMsg.getTRX();
			long weekNo = obsvMsg.getWeekNo();
			userECEF = obsvMsg.getECEF();
			double[] userLatLon = ECEFtoLatLon.ecef2lla(userECEF);
			Calendar time = Time.getDate(tRX, weekNo, userLatLon[1]);

			// find out index of nav-msg inside the nav-msg list which is most suitable for
			// each obs-msg based on time
			int order[] = obsvMsg.getObsvSat().stream().map(i -> NavMsgs.get(i.getSVID()))
					.map(i -> (ArrayList<Long>) i.stream().map(j -> j.getTOC()).collect(Collectors.toList()))
					.mapToInt(i -> Closest.findClosest(tRX, i)).toArray();

			ArrayList<Satellite> SV = new ArrayList<Satellite>();
			HashMap<Integer, Correction> PRNmap = null;
			HashMap<Integer, HashMap<Integer, Double>> IonoVDelay = null;
			if (useSBAS) {
				sbas.process(tRX);
				PRNmap = sbas.getPRNmap();
				IonoVDelay = sbas.getIonoVDelay();
				if (sbas.isIonoEnabled()) {
					Object[] xyz = IonoVDelay.keySet().toArray();
					System.out.print("");
				}

			}

			for (int i = 0; i < order.length; i++) {

				SatelliteModel sat = obsvMsg.getObsvSat().get(i);
				int SVID = sat.getSVID();
				// SBAS params
				double PRC = 0;
				LongTermCorr ltc = null;

				NavigationMsg NavMsg = NavMsgs.get(SVID).get(order[i]);
				// Incase Msg1 or PRN mask hasn't been assigned PRNmap will be null
				if (useSBAS && PRNmap != null) {
					Correction corr = PRNmap.get(SVID);
					if (corr.getFC() != null) {
						PRC = corr.getFC().getPRC();
					}
					ltc = corr.getLTC().get(NavMsg.getIODE());

				}

				IODEmap.computeIfAbsent(SVID, k -> new HashSet<Integer>()).add(NavMsg.getIODE());
				sat.setPseudorange(sat.getPseudorange() + PRC);
				double tSV = tRX - (sat.getPseudorange() / SpeedofLight);

				Object[] SatParams = ComputeSatPos.computeSatPos(NavMsg, tSV, tRX, ltc);
				double[] ECEF_SatClkOff = (double[]) SatParams[0];
				double[] SatVel = (double[]) SatParams[1];
				// Note this Clock Drift is derived, it not what we get from Ephemeris
				double SatClkDrift = (double) SatParams[2];
				// GPS System time at time of transmission time
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

			SVlist.add(SV);
			switch (estimatorType) {
			case 1:
				LS ls = new LS(SV, ionoCoeff, time);
				ErrMap.computeIfAbsent("LS", k -> new ArrayList<double[]>())
						.add(estimateError(ls.getEstECEF(), ls.getIonoCorrECEF(), userECEF, time));
				break;
			case 2:
				WLS wls = new WLS(SV, ionoCoeff, time);
				try {
					ErrMap.computeIfAbsent("WLS", k -> new ArrayList<double[]>())
							.add(estimateError(wls.getEstECEF(), wls.getTropoCorrECEF(geoid), userECEF, time));
				} catch (Exception e) {
					System.out.println(e);
				}
//				wls.computeRcvrInfo(true);
//				RcvrClkMap.computeIfAbsent("Receiver Clock Offset", k -> new ArrayList<Double>())
//						.add(wls.getRcvrClkOff());
//				RcvrClkMap.computeIfAbsent("Receiver Clock Drift", k -> new ArrayList<Double>())
//						.add(SpeedofLight * wls.getRcvrClkDrift());
				break;
			case 3:
				Doppler doppler = new Doppler(SV, ionoCoeff, time);
				ErrMap.computeIfAbsent("DopplerWLS", k -> new ArrayList<double[]>())
						.add(estimateError(doppler.getEstECEF(true), doppler.getIonoCorrECEF(true), userECEF, time));
				System.out.println("Rcvr Vel = " + doppler.getEstVel() + "  Rcvr Clk Off = " + doppler.getRcvrClkOff()
						+ "  Rcvr Clk Drift = " + doppler.getRcvrClkDrift());
				break;
			case 4:
				ls = new LS(SV, ionoCoeff, time);
				ErrMap.computeIfAbsent("LS", k -> new ArrayList<double[]>())
						.add(estimateError(ls.getEstECEF(), ls.getIonoCorrECEF(), userECEF, time));
				wls = new WLS(SV, ionoCoeff, time);
				ErrMap.computeIfAbsent("WLS", k -> new ArrayList<double[]>())
						.add(estimateError(wls.getEstECEF(), wls.getIonoCorrECEF(), userECEF, time));
//				doppler = new Doppler(SV, ionoCoeff);
//				ErrMap.computeIfAbsent("DopplerWLS", k -> new ArrayList<double[]>())
//						.add(estimateError(doppler.getEstECEF(true), doppler.getIonoCorrECEF(true), userECEF, time));
				break;
			}
			timeList.add(time);
			System.out.println();
		}

		if (estimatorType == 6) {
			for (int i = 1; i < SVlist.size(); i++) {
				DeltaRange dr = new DeltaRange(SVlist.get(i), SVlist.get(i - 1), timeList.get(i));
				ErrMap.computeIfAbsent("DR", k -> new ArrayList<double[]>())
						.add(estimateError(dr.getEstECEF(), dr.getEstECEF(ionoCoeff), userECEF, timeList.get(i)));
//				System.out.println("  Rcvr Clk Drift 1 = " + 1000 * SpeedofLight * dr.getRcvrClkDrift());
//				dr.computeRcvrInfo(userECEF, true);
//				System.out.println("  Rcvr Clk Drift 2 = " + 1000 * SpeedofLight * dr.getRcvrClkDrift());
//				double[][] weight = dr.getWeight();
//				ArrayList<Satellite> sats = dr.getSV();
//				for (int j = 0; j < sats.size(); j++) {
//					WeightMap.computeIfAbsent("" + sats.get(j).getSVID(), k -> new ArrayList<Double>())
//							.add(weight[j][j]);
//				}

			}
		}
		HashMap<String, ArrayList<Double>> GraphErrMap = new HashMap<String, ArrayList<Double>>();

		for (String key : ErrMap.keySet()) {
			ArrayList<Double> ErrList = (ArrayList<Double>) ErrMap.get(key).stream().map(i -> i[0])
					.collect(Collectors.toList());
			ArrayList<Double> IonErrList = (ArrayList<Double>) ErrMap.get(key).stream().map(i -> i[1])
					.collect(Collectors.toList());
			ArrayList<Double> LLdiffList = (ArrayList<Double>) ErrMap.get(key).stream().map(i -> i[2])
					.collect(Collectors.toList());
			ArrayList<Double> IonLLdiffList = (ArrayList<Double>) ErrMap.get(key).stream().map(i -> i[3])
					.collect(Collectors.toList());
			double minErr = Collections.min(ErrList);
			double minLLdiff = Collections.min(LLdiffList);
			double IONminErr = Collections.min(IonErrList);
			double IONminLLdiff = Collections.min(IonLLdiffList);
			System.out.println("MIN - ");
			System.out.println(minErr + " " + minLLdiff + " ION - " + IONminErr + " " + IONminLLdiff);
			System.out.println("RMS - ");
			System.out.println(
					RMS(ErrList) + " " + RMS(LLdiffList) + " ION - " + RMS(IonErrList) + " " + RMS(IonLLdiffList));
			System.out.println("MAE - ");
			System.out.println(
					MAE(ErrList) + " " + MAE(LLdiffList) + " ION - " + MAE(IonErrList) + " " + MAE(IonLLdiffList));
			GraphErrMap.put(key + " ECEF Offset", ErrList);
			GraphErrMap.put(key + " Atmos corrected ECEF Offset", IonErrList);
			GraphErrMap.put(key + " LL Offset", LLdiffList);
			GraphErrMap.put(key + " Atmos corrected LL Offset", IonLLdiffList);

		}
		if (estimatorType == 5 || estimatorType == 4) {
			ArrayList<Double> KalmanErr = new StaticEKF(SVlist, userECEF, ionoCoeff, timeList).compute(path);
			GraphErrMap.put("KALMAN ECEF", KalmanErr);

		}

		if (doIonoPlot) {

			GraphPlotter chart = new GraphPlotter("GPS IONO - ", "Iono Correction", ionoValueMap);

			chart.pack();
			RefineryUtilities.positionFrameRandomly(chart);
			chart.setVisible(true);

		}
		if (doPosErrPlot) {

			GraphPlotter chart = new GraphPlotter("GPS PVT Error - ", "Error Estimate", timeList, GraphErrMap);

			chart.pack();
			RefineryUtilities.positionFrameRandomly(chart);
			chart.setVisible(true);

		}
//		if (estimatorType == 2) {
//			GraphPlotter chart = new GraphPlotter("GPS Receiver Clock - ", "GPS Receiver Clock", timeList, RcvrClkMap);
//
//			chart.pack();
//			RefineryUtilities.positionFrameRandomly(chart);
//			chart.setVisible(true);
//		}
		if (doWeightPlot) {
			GraphPlotter chart = new GraphPlotter("Weight Matrix - ", "Weights", timeList, WeightMap);

			chart.pack();
			RefineryUtilities.positionFrameRandomly(chart);
			chart.setVisible(true);
		}
//		System.out.println("IODE MAP");
//		IODEmap.forEach((k, v) -> System.out
//				.println("PRN - " + k + " -  " + v.parallelStream().map(x -> x + " ").reduce("", (x, y) -> x + y)));
//		HashMap<Integer, Correction> _map = sbas.getPRNmap();
//		System.out.println("SBAS MAP");
//		for (int prn : _map.keySet()) {
//			System.out.println("PRN - " + prn + " - "
//					+ _map.get(prn).getLTC().keySet().parallelStream().map(x -> x + " ").reduce("", (x, y) -> x + y));
//		}
//		System.out.print("");
	}

	public static double[] estimateError(double[] nonIonoECEF, double[] IonoECEF, double[] userECEF, Calendar time) {

		double[] userLatLon = ECEFtoLatLon.ecef2lla(userECEF);

		double nonIonoError = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> userECEF[x] - nonIonoECEF[x])
				.map(x -> Math.pow(x, 2)).reduce(0, (a, b) -> a + b));
		double ionoError = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> userECEF[x] - IonoECEF[x])
				.map(x -> Math.pow(x, 2)).reduce(0, (a, b) -> a + b));
		double[] nonIonoLatLon = ECEFtoLatLon.ecef2lla(nonIonoECEF);
		double[] ionoLatLon = ECEFtoLatLon.ecef2lla(IonoECEF);

		double nonIonoDiff = LatLonDiff.getHaversineDistance(nonIonoLatLon, userLatLon);
		double ionoDiff = LatLonDiff.getHaversineDistance(ionoLatLon, userLatLon);
		// double nonIonoDiff = LatLonDiff.getVincentyDistance(nonIonoLatLon,
		// userLatLon);
		// double ionoDiff = LatLonDiff.getVincentyDistance(ionoLatLon, userLatLon);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY hh:mm:ss");

		System.out.println(sdf.format(time.getTime()) + " non Iono ECEF diff " + nonIonoError + " Iono ECEF diff "
				+ ionoError + " non Iono LL Diff - " + nonIonoDiff + " Iono LL Diff - " + ionoDiff);

		return new double[] { nonIonoError, ionoError, nonIonoDiff, ionoDiff };
	}

	public static double RMS(ArrayList<Double> list) {
		return Math.sqrt(list.stream().mapToDouble(x -> x * x).average().orElse(Double.NaN));
	}

	public static double MAE(ArrayList<Double> list) {
		return list.stream().mapToDouble(x -> x).average().orElse(Double.NaN);
	}

	public static Geoid buildGeoid() {
		// Semi-major axis or Equatorial radius
		final double ae = 6378137;
		// flattening
		final double f = 1 / 298.257223563;

		// Earth's rotation rate
		final double spin = 7.2921151467E-5;
		// Earth's universal gravitational parameter
		final double GM = 3.986004418E14;

		File orekitData = new File(
				"C:\\Users\\Naman\\Desktop\\rinex_parse_files\\orekit\\orekit-data-master\\orekit-data-master");
		DataProvidersManager manager = DataProvidersManager.getInstance();
		manager.addProvider(new DirectoryCrawler(orekitData));
		NormalizedSphericalHarmonicsProvider nhsp = GravityFieldFactory.getNormalizedProvider(50, 50);
		Frame frame = FramesFactory.getITRF(ITRFVersion.ITRF_2014, IERSConventions.IERS_2010, true);

		// ReferenceEllipsoid refElp = new ReferenceEllipsoid(ae, f, frame, GM, spin);
		Geoid geoid = new Geoid(nhsp, ReferenceEllipsoid.getWgs84(frame));
		return geoid;

	}

}
