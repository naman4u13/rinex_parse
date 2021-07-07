package com.RINEX_parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.stream.IntStream;

import org.jfree.ui.RefineryUtilities;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.Geoid;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.utils.IERSConventions;

import com.RINEX_parser.ComputeUserPos.KalmanFilter.EKF;
import com.RINEX_parser.ComputeUserPos.Regression.LS;
import com.RINEX_parser.ComputeUserPos.Regression.WLS;
import com.RINEX_parser.GoogleDecimeter.GoogleDeciApp;
import com.RINEX_parser.fileParser.Antenna;
import com.RINEX_parser.fileParser.Bias;
import com.RINEX_parser.fileParser.Clock;
import com.RINEX_parser.fileParser.IONEX;
import com.RINEX_parser.fileParser.NavigationRNX;
import com.RINEX_parser.fileParser.ObservationRNX;
import com.RINEX_parser.fileParser.Orbit;
import com.RINEX_parser.fileParser.RTKlib;
import com.RINEX_parser.fileParser.SBAS;
import com.RINEX_parser.helper.CycleSlip;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.IonoValue;
import com.RINEX_parser.models.NavigationMsg;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.TimeCorrection;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.GoogleData;
import com.RINEX_parser.utility.GraphPlotter;
import com.RINEX_parser.utility.IGPgrid;
import com.RINEX_parser.utility.LatLonUtil;
import com.RINEX_parser.utility.Time;
import com.opencsv.CSVWriter;

public class MainApp {

	public static void main(String[] args) {
		try {
			CSVWriter writer = null;
			String filePath = "E:\\Study\\Google Decimeter Challenge\\decimeter\\test\\2020-05-28-US-MTV-1\\Pixel4XL\\rinexObs.csv";
			String[] header = new String[] { "MillisSinceGpsEpoch" };
			String obs_path = "E:\\Study\\Google Decimeter Challenge\\decimeter\\test\\2020-05-28-US-MTV-1\\Pixel4XL\\supplemental\\Pixel4XL_GnssLog.20o";
			HashMap<String, Object> ObsvMsgComp;
			File file = new File(filePath);
			// create FileWriter object with file as parameter
			FileWriter outputfile;

			outputfile = new FileWriter(file);

			// create CSVWriter object filewriter object as parameter
			writer = new CSVWriter(outputfile);
			writer.writeNext(header);
			ObsvMsgComp = ObservationRNX.rinex_obsv_process(obs_path, false, null, new String[] { "G1C" }, false);
			@SuppressWarnings("unchecked")
			ArrayList<ObservationMsg> ObsvMsgs = (ArrayList<ObservationMsg>) ObsvMsgComp.get("ObsvMsgs");
			for (ObservationMsg obs : ObsvMsgs) {
				long tRX = (long) Math.floor((obs.getTRX() + (604800 * obs.getWeekNo())) * 1000);
				writer.writeNext(new String[] { tRX + "" });
			}
			writer.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main2(String[] args) {

		Instant start = Instant.now();

		switch (3) {
		case 1:
			/*
			 * public static void posEstimate(boolean doWeightPlot, boolean doIonoPlot,
			 * boolean doPosErrPlot, boolean useCutOffAng, boolean useSNX, boolean useSBAS,
			 * boolean useBias, boolean useIGS, boolean isDual, boolean useGIM, boolean
			 * useRTKlib, boolean usePhase, int estimatorType, String[] obsvCode, int
			 * minSat)
			 */
			posEstimate(false, false, true, true, true, false, true, true, true, false, false, false, 4,
					new String[] { "G1C", "G2X" }, 4);
			break;

		case 2:
			/*
			 * posEstimate(boolean doPosErrPlot, boolean useCutOffAng, boolean useBias,
			 * boolean useIGS, boolean isDual, boolean useGIM, boolean useRTKlib, boolean
			 * usePhase, int estimatorType, String[] obsvCode, int minSat, String obs_path,
			 * String derived_csv_path,String[] obsvCodeList)
			 * 
			 */

			String[] obsvCodeList = new String[] { "G1C", "E1C", "C2I", "J1C" };
			String obs_path = "E:\\Study\\Google Decimeter Challenge\\decimeter\\train\\2021-04-29-US-SJC-2\\SamsungS20Ultra\\supplemental\\SamsungS20Ultra_GnssLog.21o";
			String derived_csv_path = "E:\\Study\\Google Decimeter Challenge\\decimeter\\train\\2021-04-29-US-SJC-2\\SamsungS20Ultra\\SamsungS20Ultra_derived.csv";
			GoogleDeciApp.posEstimate(true, true, false, false, false, false, false, false, 3, new String[] { "G1C" },
					4, obs_path, derived_csv_path, obsvCodeList);
			break;
		case 3:
//			File output = new File("E:\\Study\\Google Decimeter Challenge\\decimeter\\errReport2.txt");
//			PrintStream stream;

			try {
//				stream = new PrintStream(output);
//				System.setErr(stream);
				CSVWriter writer = null;
				String filePath = "E:\\Study\\Google Decimeter Challenge\\result\\test2.csv";
				String[] header = new String[] { "phone", "millisSinceGpsEpoch", "latDeg", "lngDeg" };
				obsvCodeList = new String[] { "G1C" };
				File file = new File(filePath);
				// create FileWriter object with file as parameter
				FileWriter outputfile;

				outputfile = new FileWriter(file);

				// create CSVWriter object filewriter object as parameter
				writer = new CSVWriter(outputfile);
				writer.writeNext(header);
				File testDir = new File("E:\\Study\\Google Decimeter Challenge\\decimeter\\test");
				File[] dayFiles = testDir.listFiles();
				for (File dayFile : dayFiles) {
					File[] mobFiles = dayFile.listFiles();
					for (File mobFile : mobFiles) {
						String mobName = mobFile.getName();
						String path = mobFile.getAbsolutePath();
						String year = dayFile.getName().split("-")[0].substring(2);
						obs_path = path + "\\supplemental\\" + mobName + "_GnssLog." + year + "o";

						derived_csv_path = path + "\\" + mobName + "_derived.csv";
						ArrayList<String[]> csvRes = GoogleDeciApp.posEstimate(true, true, false, false, false, false,
								false, false, 6, new String[] { "G1C" }, 4, obs_path, null, obsvCodeList);
						csvRes.stream().forEach(i -> i[0] = dayFile.getName() + "_" + mobName);
						csvRes = GoogleData.predict(csvRes);
						// GoogleData.filter(csvRes);
						writer.writeAll(csvRes);

					}
				}
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		}

		Instant end = Instant.now();
		System.out.println("EXECUTION TIME -  " + Duration.between(start, end));

	}

	public static void posEstimate(boolean doWeightPlot, boolean doIonoPlot, boolean doPosErrPlot, boolean useCutOffAng,
			boolean useSNX, boolean useSBAS, boolean useBias, boolean useIGS, boolean isDual, boolean useGIM,
			boolean useRTKlib, boolean usePhase, int estimatorType, String[] obsvCode, int minSat) {
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			HashMap<Integer, ArrayList<IonoValue>> ionoValueMap = new HashMap<Integer, ArrayList<IonoValue>>();

			HashMap<String, ArrayList<HashMap<String, double[]>>> ErrMap = new HashMap<String, ArrayList<HashMap<String, double[]>>>();
			HashMap<String, ArrayList<Double>> RcvrClkMap = new HashMap<String, ArrayList<Double>>();
			HashMap<String, ArrayList<Double>> WeightMap = new HashMap<String, ArrayList<Double>>();
			ArrayList<Calendar> timeList = new ArrayList<Calendar>();
			ArrayList<Double> GPSTimeList = new ArrayList<Double>();
			ArrayList<ArrayList<Satellite>> SVlist = new ArrayList<ArrayList<Satellite>>();
			ArrayList<ArrayList<Satellite>[]> dualSVlist = new ArrayList<ArrayList<Satellite>[]>();
			ArrayList<String[]> IPPdelay = new ArrayList<String[]>();

			SBAS sbas = null;
			Bias bias = null;
			HashMap<Integer, HashSet<Integer>> IODEmap = new HashMap<Integer, HashSet<Integer>>();
			Orbit orbit = null;
			Clock clock = null;
			Antenna antenna = null;
			IONEX ionex = null;
			RTKlib rtkLib = null;

			String nav_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\BRDC00IGS_R_20201000000_01D_MN.rnx\\BRDC00IGS_R_20201000000_01D_MN.rnx";

			String obs_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\NYA100NOR_S_20201000000_01D_30S_MO.rnx\\NYA100NOR_S_20201000000_01D_30S_MO.rnx";

			String sbas_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\EGNOS_2020_100\\123\\D100.ems";

			String bias_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\CAS0MGXRAP_20201000000_01D_01D_DCB.BSX\\CAS0MGXRAP_20201000000_01D_01D_DCB.BSX";

			String orbit_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\igs21004.sp3\\igs21004.sp3";

			String sinex_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\igs20P21004.snx\\igs20P21004.snx";

			String antenna_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\igs14.atx\\igs14.atx";

			String antenna_csv_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\antenna.csv";

			String clock_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\igs21004.clk_30s\\igs21004.clk_30s";

			String ionex_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\igsg1000.20i\\igsg1000.20i";

			String RTKlib_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\RTKlib\\AGGO_GIM_PPP.pos";

			String path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\RTKlib_compare\\test";
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
					obsvCode, usePhase);
			@SuppressWarnings("unchecked")
			ArrayList<ObservationMsg> ObsvMsgs = (ArrayList<ObservationMsg>) ObsvMsgComp.get("ObsvMsgs");
			double[] rxARP = (double[]) ObsvMsgComp.get("ARP");
			double[][] rxPCO = (double[][]) ObsvMsgComp.get("PCO");

			int interval = (int) ObsvMsgComp.get("interval");
			// Note PVT algos will compute for Antenna Reference Point as it is independent
			// of frequency
			double[] userECEF = rxARP;
			double[] userLatLon = ECEFtoLatLon.ecef2lla(userECEF);

			TopocentricFrame tpf = new TopocentricFrame(geoid,
					new GeodeticPoint(Math.toRadians(userLatLon[0]), Math.toRadians(userLatLon[1]), userLatLon[2]),
					"TPF");
			Frame frame = FramesFactory.getITRF(ITRFVersion.ITRF_2014, IERSConventions.IERS_2010, true);

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
			if (useGIM) {
				ionex = new IONEX(ionex_path);

			}

			if (useRTKlib) {

				rtkLib = new RTKlib(RTKlib_path);

				ErrMap.put("RTKlib", new ArrayList<HashMap<String, double[]>>());

			}

			long epochStart = 7200;
			long epochEnd = 79200;

			for (ObservationMsg obsvMsg : ObsvMsgs) {

				double tRX = obsvMsg.getTRX();

				double dayTime = tRX % 86400;
				if (dayTime < epochStart) {
					continue;
				} else if (dayTime > epochEnd) {
					break;
				}
				if (dayTime % 10 != 0) {
					System.err.println("FATAL ERROR TIME daytime");
					// return;
				}
				long weekNo = obsvMsg.getWeekNo();

				Calendar time = Time.getDate(tRX, weekNo, 0);
				if (Time.getGPSTime(time)[0] != tRX) {
					System.err.println("FATAL ERROR TIME calendar");
					return;
				}
				ArrayList<Satellite>[] dualSV = null;
				ArrayList<Satellite> SV = null;
				if (isDual) {
					dualSV = DualFreq.process(obsvMsg, NavMsgs, obsvCode, useIGS, useBias, bias, orbit, clock, antenna,
							tRX, weekNo, userECEF, time, useCutOffAng);
					if (dualSV[0].size() < minSat && dualSV[1].size() < minSat) {
						continue;
					}
					dualSVlist.add(dualSV);
				} else {
					SV = SingleFreq.process(obsvMsg, NavMsgs, obsvCode[0], useIGS, useSBAS, doIonoPlot, useBias,
							ionoCoeff, bias, orbit, clock, antenna, tRX, weekNo, time, sbas, userECEF, userLatLon,
							ionoValueMap, useCutOffAng, tpf, frame);
					if (SV.size() < minSat) {
						continue;
					}
					SVlist.add(SV);
				}

				switch (estimatorType) {
				case 1:
					LS ls = null;
					com.RINEX_parser.ComputeUserPos.Regression.DualFreq.LS dualLS = null;
					if (isDual) {
						dualLS = new com.RINEX_parser.ComputeUserPos.Regression.DualFreq.LS(dualSV, rxPCO, userECEF,
								time, geoid);

						ErrMap.computeIfAbsent("dual-LS", k -> new ArrayList<HashMap<String, double[]>>())
								.add(estimateError(
										Map.of("IonoCorr", dualLS.getEstECEF(), "TropoCorr", dualLS.getTropoCorrECEF()),
										userECEF, time));

					} else {
						ls = new LS(SV, rxPCO[0], ionoCoeff, time, ionex, geoid);

						ErrMap.computeIfAbsent("LS", k -> new ArrayList<HashMap<String, double[]>>())
								.add(estimateError(Map.of("Simple", ls.getEstECEF(), "IonoCorr ", ls.getIonoCorrECEF(),
										"TropoCorr", ls.getTropoCorrECEF()), userECEF, time));

					}

					break;
				case 2:
					WLS wls = null;
					com.RINEX_parser.ComputeUserPos.Regression.DualFreq.WLS dualWLS = null;
					if (isDual) {
						dualWLS = new com.RINEX_parser.ComputeUserPos.Regression.DualFreq.WLS(dualSV, rxPCO, userECEF,
								time, geoid);
						ErrMap.computeIfAbsent("dual-WLS", k -> new ArrayList<HashMap<String, double[]>>())
								.add(estimateError(Map.of("IonoCorr", dualWLS.getEstECEF(), "TropoCorr",
										dualWLS.getTropoCorrECEF()), userECEF, time));

					} else {
						wls = new WLS(SV, rxPCO[0], ionoCoeff, time, ionex, geoid);
						ErrMap.computeIfAbsent("WLS", k -> new ArrayList<HashMap<String, double[]>>())
								.add(estimateError(Map.of("Simple", wls.getEstECEF(), "IonoCorr ",
										wls.getIonoCorrECEF(), "TropoCorr", wls.getTropoCorrECEF()), userECEF, time));
					}

					break;

				case 3:
					ls = null;
					wls = null;
					dualLS = null;
					dualWLS = null;

					if (isDual) {
						dualLS = new com.RINEX_parser.ComputeUserPos.Regression.DualFreq.LS(dualSV, rxPCO, userECEF,
								time, geoid);
						dualWLS = new com.RINEX_parser.ComputeUserPos.Regression.DualFreq.WLS(dualSV, rxPCO, userECEF,
								time, geoid);
						ErrMap.computeIfAbsent("dual-LS", k -> new ArrayList<HashMap<String, double[]>>())
								.add(estimateError(
										Map.of("IonoCorr", dualLS.getEstECEF(), "TropoCorr", dualLS.getTropoCorrECEF()),
										userECEF, time));
						ErrMap.computeIfAbsent("dual-WLS", k -> new ArrayList<HashMap<String, double[]>>())
								.add(estimateError(Map.of("IonoCorr", dualWLS.getEstECEF(), "TropoCorr",
										dualWLS.getTropoCorrECEF()), userECEF, time));

					} else {
						ls = new LS(SV, rxPCO[0], ionoCoeff, time, ionex, geoid);
						wls = new WLS(SV, rxPCO[0], ionoCoeff, time, ionex, geoid);
						ErrMap.computeIfAbsent("LS", k -> new ArrayList<HashMap<String, double[]>>())
								.add(estimateError(Map.of("Simple", ls.getEstECEF(), "IonoCorr ", ls.getIonoCorrECEF(),
										"TropoCorr", ls.getTropoCorrECEF()), userECEF, time));

						ErrMap.computeIfAbsent("WLS", k -> new ArrayList<HashMap<String, double[]>>())
								.add(estimateError(Map.of("Simple", wls.getEstECEF(), "IonoCorr ",
										wls.getIonoCorrECEF(), "TropoCorr", wls.getTropoCorrECEF()), userECEF, time));
					}
					break;

				}
				GPSTimeList.add(tRX);
				timeList.add(time);

			}

			if (estimatorType == 4) {

				if (isDual) {
					if (usePhase) {
						CycleSlip cs = new CycleSlip(interval);
						cs.process(dualSVlist);
					}
					ErrMap.put("dual-EKF", new ArrayList<HashMap<String, double[]>>());
					WLS wls = new WLS(dualSVlist.get(0)[0], rxPCO[0], ionoCoeff, timeList.get(0), ionex, geoid);
					com.RINEX_parser.ComputeUserPos.KalmanFilter.DualFreq.StaticEKF dualStaticEkf = new com.RINEX_parser.ComputeUserPos.KalmanFilter.DualFreq.StaticEKF(
							dualSVlist, rxPCO, wls.getTropoCorrECEF(), userECEF, geoid, timeList, usePhase);
					ArrayList<double[]> estECEFList = dualStaticEkf.compute();
					for (int i = 0; i < dualSVlist.size() - 1; i++) {
						double[] estECEF = estECEFList.get(i);
						ErrMap.get("dual-EKF")
								.add(estimateError(Map.of("TropoCorr", estECEF), userECEF, timeList.get(i)));

					}
				} else {
					ErrMap.put("EKF", new ArrayList<HashMap<String, double[]>>());
					EKF staticEkf = new EKF(SVlist, rxPCO[0], userECEF, ionoCoeff, ionex, geoid, timeList);
					ArrayList<double[]> estECEFList = staticEkf.computeStatic();
					for (int i = 0; i < SVlist.size() - 1; i++) {
						double[] estECEF = estECEFList.get(i);
						ErrMap.get("EKF").add(estimateError(Map.of("TropoCorr", estECEF), userECEF, timeList.get(i)));
					}
				}

			}
			if (estimatorType == 5) {
				CycleSlip cs = new CycleSlip(interval);
//				int svid = 1;
//				for (ArrayList<Satellite>[] SV : dualSVlist) {
//					SV[0] = SV[0].stream().filter(i -> i.getSVID() == svid)
//							.collect(Collectors.toCollection(ArrayList::new));
//					SV[1] = SV[1].stream().filter(i -> i.getSVID() == svid)
//							.collect(Collectors.toCollection(ArrayList::new));
//				}
//				dualSVlist = dualSVlist.stream().filter(i -> i[0].size() > 0)
//						.collect(Collectors.toCollection(ArrayList::new));
				HashMap<Integer, int[]> csMap = cs.process(dualSVlist);

				for (int SVID : csMap.keySet()) {
					int[] data = csMap.get(SVID);
					GraphPlotter chart = new GraphPlotter("Cycle Slip - " + SVID, "Cycle Slip - " + SVID, data,
							timeList, SVID);

					chart.pack();
					RefineryUtilities.positionFrameRandomly(chart);
					chart.setVisible(true);
				}

			}
			if (useRTKlib) {
				int len = GPSTimeList.size();
				int start = rtkLib.getIndex(GPSTimeList.get(0));
				int end = rtkLib.getIndex(GPSTimeList.get(len - 1));
				String posMode = rtkLib.getPosMode();
				for (int i = start; i < end; i++) {

					ErrMap.get("RTKlib")
							.add(estimateError(Map.of(posMode, rtkLib.getECEF(i)), userECEF, timeList.get(i - start)));
				}
			}

			HashMap<String, ArrayList<Double>> GraphErrMap = new HashMap<String, ArrayList<Double>>();

			for (String key : ErrMap.keySet()) {

				HashMap<String, ArrayList<Double>> ecefErrMap = new HashMap<String, ArrayList<Double>>();
				HashMap<String, ArrayList<Double>> llErrMap = new HashMap<String, ArrayList<Double>>();
				ArrayList<HashMap<String, double[]>> errMap = ErrMap.get(key);
				for (HashMap<String, double[]> map : errMap) {
					for (String subKey : map.keySet()) {
						double[] err = map.get(subKey);
						ecefErrMap.computeIfAbsent(subKey, k -> new ArrayList<Double>()).add(err[0]);
						llErrMap.computeIfAbsent(subKey, k -> new ArrayList<Double>()).add(err[1]);
					}
				}
				String header = "order - ";
				String minECEF = "ECEF - ";
				String minLL = "LL - ";
				String rmsECEF = "ECEF - ";
				String maeECEF = "ECEF - ";
				String rmsLL = "LL - ";
				String maeLL = "LL - ";

				for (String subKey : ecefErrMap.keySet()) {
					ArrayList<Double> ecefErrList = ecefErrMap.get(subKey);
					ArrayList<Double> llErrList = llErrMap.get(subKey);
					header += subKey + " ";
					minECEF += Collections.min(ecefErrList) + " ";
					minLL += Collections.min(llErrList) + " ";
					rmsECEF += RMS(ecefErrList) + " ";
					rmsLL += RMS(llErrList) + " ";
					maeECEF += MAE(ecefErrList) + " ";
					maeLL += MAE(llErrList) + " ";
					GraphErrMap.put(key + " " + subKey + " ECEF off", ecefErrList);
					GraphErrMap.put(key + " " + subKey + " LL off", llErrList);

				}
				System.out.println("\n" + key);
				System.out.println(header);
				System.out.println("MIN - ");
				System.out.println(minECEF);
				System.out.println(minLL);
				System.out.println("RMS - ");
				System.out.println(rmsECEF);
				System.out.println(rmsLL);
				System.out.println("MAE - ");
				System.out.println(maeECEF);
				System.out.println(maeLL);

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

			if (doWeightPlot) {
				GraphPlotter chart = new GraphPlotter("Weight Matrix - ", "Weights", timeList, WeightMap);

				chart.pack();
				RefineryUtilities.positionFrameRandomly(chart);
				chart.setVisible(true);
			}

		} catch (

		Exception e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public static HashMap<String, double[]> estimateError(Map<String, double[]> ECEFmap, double[] userECEF,
			Calendar time) {
		double[] userLatLon = ECEFtoLatLon.ecef2lla(userECEF);

		HashMap<String, double[]> errMap = new HashMap<String, double[]>();

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY hh:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String errStr = sdf.format(time.getTime());
		for (String key : ECEFmap.keySet()) {
			double[] estECEF = ECEFmap.get(key);
			double ecefErr = Math.sqrt(IntStream.range(0, 3).mapToDouble(x -> userECEF[x] - estECEF[x])
					.map(x -> Math.pow(x, 2)).reduce(0, (a, b) -> a + b));
			double[] estLL = ECEFtoLatLon.ecef2lla(estECEF);
			// Great Circle Distance
			double gcErr = LatLonUtil.getHaversineDistance(estLL, userLatLon);
			errMap.put(key, new double[] { ecefErr, gcErr });
			errStr += " " + key + " ECEF diff " + ecefErr + " " + key + " LL diff " + gcErr;

		}
		System.out.println(errStr);
		System.out.println();
		return errMap;
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
