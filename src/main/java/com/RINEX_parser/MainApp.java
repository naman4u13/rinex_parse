package com.RINEX_parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
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
import com.RINEX_parser.fileParser.Antenna;
import com.RINEX_parser.fileParser.Bias;
import com.RINEX_parser.fileParser.Clock;
import com.RINEX_parser.fileParser.NavigationRNX;
import com.RINEX_parser.fileParser.ObservationRNX;
import com.RINEX_parser.fileParser.Orbit;
import com.RINEX_parser.fileParser.SBAS;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.IonoValue;
import com.RINEX_parser.models.NavigationMsg;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.TimeCorrection;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.GraphPlotter;
import com.RINEX_parser.utility.IGPgrid;
import com.RINEX_parser.utility.LatLonUtil;
import com.RINEX_parser.utility.Time;

public class MainApp {

	public static void main(String[] args) {

		Instant start = Instant.now();
		posEstimate(false, false, true, true, false, true, false, false, 4, new String[] { "G1C" });// , "G2X" });
		Instant end = Instant.now();
		System.out.println("EXECUTION TIME -  " + Duration.between(start, end));

	}

	public static void posEstimate(boolean doWeightPlot, boolean doIonoPlot, boolean doPosErrPlot, boolean useSNX,
			boolean useSBAS, boolean useBias, boolean useIGS, boolean isDual, int estimatorType, String[] obsvCode) {
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			HashMap<Integer, ArrayList<IonoValue>> ionoValueMap = new HashMap<Integer, ArrayList<IonoValue>>();
			HashMap<String, ArrayList<double[]>> ErrMap = new HashMap<String, ArrayList<double[]>>();
			HashMap<String, ArrayList<Double>> RcvrClkMap = new HashMap<String, ArrayList<Double>>();
			HashMap<String, ArrayList<Double>> WeightMap = new HashMap<String, ArrayList<Double>>();
			ArrayList<Calendar> timeList = new ArrayList<Calendar>();
			ArrayList<ArrayList<Satellite>> SVlist = new ArrayList<ArrayList<Satellite>>();

			ArrayList<String[]> IPPdelay = new ArrayList<String[]>();
			SBAS sbas = null;
			Bias bias = null;
			HashMap<Integer, HashSet<Integer>> IODEmap = new HashMap<Integer, HashSet<Integer>>();
			Orbit orbit = null;
			Clock clock = null;
			Antenna antenna = null;

			String nav_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\BRDC00IGS_R_20201000000_01D_MN.rnx\\BRDC00IGS_R_20201000000_01D_MN.rnx";

			String obs_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\MADR00ESP_R_20201001000_01H_30S_MO.crx\\MADR00ESP_R_20201001000_01H_30S_MO.rnx";

			String sbas_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\EGNOS_2020_100\\123\\D100.ems";

			String bias_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\CAS0MGXRAP_20201000000_01D_01D_DCB.BSX\\CAS0MGXRAP_20201000000_01D_01D_DCB.BSX";

			String orbit_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\igs21004.sp3\\igs21004.sp3";

			String sinex_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\igs20P21004.snx\\igs20P21004.snx";

			String antenna_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\igs14.atx\\igs14.atx";

			String antenna_csv_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\antenna.csv";

			String clock_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\igs21004.clk_30s\\igs21004.clk_30s";

			String path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\PPPres\\test2";
			File output = new File(path + ".txt");
			PrintStream stream;

			try {
				stream = new PrintStream(output);
				System.setOut(stream);
			} catch (FileNotFoundException e) { // TODO Auto-generated catch block
				e.printStackTrace();
			}

			Geoid geoid = buildGeoid();
			int fN = obsvCode.length;
			Map<String, Object> NavMsgComp = NavigationRNX.rinex_nav_process(nav_path, useIGS);
			@SuppressWarnings("unchecked")
			HashMap<Integer, ArrayList<NavigationMsg>> NavMsgs = (HashMap<Integer, ArrayList<NavigationMsg>>) NavMsgComp
					.getOrDefault("NavMsgs", null);
			IonoCoeff ionoCoeff = (IonoCoeff) NavMsgComp.get("ionoCoeff");
			TimeCorrection timeCorr = (TimeCorrection) NavMsgComp.getOrDefault("timeCorr", null);
			HashMap<String, Object> ObsvMsgComp = ObservationRNX.rinex_obsv_process(obs_path, useSNX, sinex_path,
					obsvCode);
			@SuppressWarnings("unchecked")
			ArrayList<ObservationMsg> ObsvMsgs = (ArrayList<ObservationMsg>) ObsvMsgComp.get("ObsvMsgs");
			double[] rxARP = (double[]) ObsvMsgComp.get("ARP");
			double[][] rxPCO = (double[][]) ObsvMsgComp.get("PCO");

			// Note PVT algos will compute for Antenna Reference Point as it is independent
			// of frequency
			double[] userECEF = rxARP;
			double[] userLatLon = ECEFtoLatLon.ecef2lla(userECEF);

			if (useSBAS) {
				int[][][] IGP = IGPgrid.readCSV();
				sbas = new SBAS(sbas_path, IGP);
			}

			if (useBias) {
				bias = new Bias(bias_path);

			}
			if (useIGS) {

				char SSI = obsvCode[0].charAt(0);
				orbit = new Orbit(orbit_path, SSI);
				clock = new Clock(clock_path, bias);
				antenna = new Antenna(antenna_csv_path);

			}

			for (ObservationMsg obsvMsg : ObsvMsgs) {

				long tRX = obsvMsg.getTRX();
				long dayTime = tRX % 86400;
				if (dayTime < 7200 || dayTime > 79200) {
					continue;
				}
				long weekNo = obsvMsg.getWeekNo();

				Calendar time = Time.getDate(tRX, weekNo, userLatLon[1]);
				timeList.add(time);
				ArrayList<Satellite> SV = new ArrayList<Satellite>();
				if (isDual) {
					DualFreq.process(obsvMsg, NavMsgs, obsvCode, useIGS, useBias, bias, orbit, clock, antenna, tRX,
							weekNo, time);
				} else {
					SV = SingleFreq.process(obsvMsg, NavMsgs, obsvCode[0], useIGS, useSBAS, doIonoPlot, useBias,
							ionoCoeff, bias, orbit, clock, antenna, tRX, weekNo, time, sbas, userECEF, userLatLon,
							ionoValueMap);
				}

				SVlist.add(SV);
				switch (estimatorType) {
				case 1:
					LS ls = null;
					if (isDual) {

					} else {
						ls = new LS(SV, rxPCO[0], ionoCoeff, time);
					}

					ErrMap.computeIfAbsent("LS", k -> new ArrayList<double[]>())
							.add(estimateError(ls.getIonoCorrECEF(), ls.getTropoCorrECEF(geoid), userECEF, time));
					break;
				case 2:
					WLS wls = null;
					if (isDual) {

					} else {
						wls = new WLS(SV, rxPCO[0], ionoCoeff, time);
					}
					try {

						ErrMap.computeIfAbsent("WLS", k -> new ArrayList<double[]>())
								.add(estimateError(wls.getIonoCorrECEF(), wls.getTropoCorrECEF(geoid), userECEF, time));
					} catch (Exception e) {
						System.out.println(e);
					}

					break;
				case 3:
					Doppler doppler = new Doppler(SV, rxPCO[0], ionoCoeff, time);
					ErrMap.computeIfAbsent("DopplerWLS", k -> new ArrayList<double[]>()).add(
							estimateError(doppler.getEstECEF(true), doppler.getIonoCorrECEF(true), userECEF, time));
					System.out.println("Rcvr Vel = " + doppler.getEstVel() + "  Rcvr Clk Off = "
							+ doppler.getRcvrClkOff() + "  Rcvr Clk Drift = " + doppler.getRcvrClkDrift());
					break;
				case 4:
					ls = null;
					wls = null;
					if (isDual) {

					} else {
						ls = new LS(SV, rxPCO[0], ionoCoeff, time);
						wls = new WLS(SV, rxPCO[0], ionoCoeff, time);
					}
					ErrMap.computeIfAbsent("LS", k -> new ArrayList<double[]>())
							.add(estimateError(ls.getEstECEF(), ls.getTropoCorrECEF(geoid), userECEF, time));

					ErrMap.computeIfAbsent("WLS", k -> new ArrayList<double[]>())
							.add(estimateError(wls.getEstECEF(), wls.getTropoCorrECEF(geoid), userECEF, time));
//				doppler = new Doppler(SV, ionoCoeff);
//				ErrMap.computeIfAbsent("DopplerWLS", k -> new ArrayList<double[]>())
//						.add(estimateError(doppler.getEstECEF(true), doppler.getIonoCorrECEF(true), userECEF, time));
					break;
				}

				System.out.println();
			}
//		IGPgrid.recordIPPdelay(IPPdelay);
			if (estimatorType == 6) {
				for (int i = 1; i < SVlist.size(); i++) {
					DeltaRange dr = new DeltaRange(SVlist.get(i), SVlist.get(i - 1), rxPCO[0], timeList.get(i));
					ErrMap.computeIfAbsent("DR", k -> new ArrayList<double[]>())
							.add(estimateError(dr.getEstECEF(), dr.getEstECEF(ionoCoeff), userECEF, timeList.get(i)));

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
			if (estimatorType == 5) {
				ArrayList<Double> KalmanErr = new StaticEKF(SVlist, rxPCO[0], userECEF, ionoCoeff, timeList)
						.compute(path);
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
		} catch (

		Exception e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public static double[] estimateError(double[] nonIonoECEF, double[] IonoECEF, double[] userECEF, Calendar time) {

		double[] userLatLon = ECEFtoLatLon.ecef2lla(userECEF);

		double nonIonoError = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> userECEF[x] - nonIonoECEF[x])
				.map(x -> Math.pow(x, 2)).reduce(0, (a, b) -> a + b));
		double ionoError = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> userECEF[x] - IonoECEF[x])
				.map(x -> Math.pow(x, 2)).reduce(0, (a, b) -> a + b));
		double[] nonIonoLatLon = ECEFtoLatLon.ecef2lla(nonIonoECEF);
		double[] ionoLatLon = ECEFtoLatLon.ecef2lla(IonoECEF);

		double nonIonoDiff = LatLonUtil.getHaversineDistance(nonIonoLatLon, userLatLon);
		double ionoDiff = LatLonUtil.getHaversineDistance(ionoLatLon, userLatLon);
		// double nonIonoDiff = LatLonUtil.getVincentyDistance(nonIonoLatLon,
		// userLatLon);
		// double ionoDiff = LatLonUtil.getVincentyDistance(ionoLatLon, userLatLon);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY hh:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		System.out.println(sdf.format(time.getTime()) + " non Iono ECEF diff " + nonIonoError + " Iono ECEF diff "
				+ ionoError + " non Iono LL Diff - " + nonIonoDiff + " Iono LL Diff - " + ionoDiff);

		return new double[] { nonIonoError, ionoError, nonIonoDiff, ionoDiff };
	}

	public static double[] estimateError(double[] ECEF1, double[] ECEF2, double[] ECEF3, double[] userECEF,
			Calendar time) {

		double[] userLatLon = ECEFtoLatLon.ecef2lla(userECEF);

		double ErrXYZ1 = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> userECEF[x] - ECEF1[x])
				.map(x -> Math.pow(x, 2)).reduce(0, (a, b) -> a + b));
		double ErrXYZ2 = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> userECEF[x] - ECEF2[x])
				.map(x -> Math.pow(x, 2)).reduce(0, (a, b) -> a + b));
		double ErrXYZ3 = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> userECEF[x] - ECEF3[x])
				.map(x -> Math.pow(x, 2)).reduce(0, (a, b) -> a + b));

		double[] LL1 = ECEFtoLatLon.ecef2lla(ECEF1);
		double[] LL2 = ECEFtoLatLon.ecef2lla(ECEF2);
		double[] LL3 = ECEFtoLatLon.ecef2lla(ECEF3);

		double ErrLL1 = LatLonUtil.getHaversineDistance(LL1, userLatLon);
		double ErrLL2 = LatLonUtil.getHaversineDistance(LL2, userLatLon);
		double ErrLL3 = LatLonUtil.getHaversineDistance(LL3, userLatLon);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY hh:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		System.out.println(sdf.format(time.getTime()) + " ECEF1 diff " + ErrXYZ1 + " ECEF2 diff " + ErrXYZ2
				+ " ECEF3 diff " + ErrXYZ3 + " LL1 diff " + ErrLL1 + " LL2 diff " + ErrLL2 + " LL3 diff " + ErrLL3);
		return new double[] { ErrXYZ1, ErrXYZ2, ErrXYZ3, ErrLL1, ErrLL2, ErrLL3 };
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
