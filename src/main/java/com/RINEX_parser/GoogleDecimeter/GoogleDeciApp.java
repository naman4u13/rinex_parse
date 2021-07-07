package com.RINEX_parser.GoogleDecimeter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

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

import com.RINEX_parser.DualFreq;
import com.RINEX_parser.SingleFreq;
import com.RINEX_parser.ComputeUserPos.KalmanFilter.EKF;
import com.RINEX_parser.ComputeUserPos.Regression.LS;
import com.RINEX_parser.ComputeUserPos.Regression.WLS;
import com.RINEX_parser.fileParser.Antenna;
import com.RINEX_parser.fileParser.Bias;
import com.RINEX_parser.fileParser.Clock;
import com.RINEX_parser.fileParser.IONEX;
import com.RINEX_parser.fileParser.NavigationRNX;
import com.RINEX_parser.fileParser.ObservationRNX;
import com.RINEX_parser.fileParser.Orbit;
import com.RINEX_parser.fileParser.RTKlib;
import com.RINEX_parser.fileParser.GoogleDecimeter.DerivedCSV;
import com.RINEX_parser.fileParser.GoogleDecimeter.GroundTruth;
import com.RINEX_parser.helper.CycleSlip;
import com.RINEX_parser.models.IonoCoeff;
import com.RINEX_parser.models.IonoValue;
import com.RINEX_parser.models.NavigationMsg;
import com.RINEX_parser.models.ObservationMsg;
import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.models.GoogleDecimeter.Derived;
import com.RINEX_parser.utility.ECEFtoLatLon;
import com.RINEX_parser.utility.GraphPlotter;
import com.RINEX_parser.utility.LatLonUtil;
import com.RINEX_parser.utility.Time;

public class GoogleDeciApp {

	public static final long milliSecInWeek = 604800000;

	@SuppressWarnings("unchecked")
	public static ArrayList<String[]> posEstimate(boolean doPosErrPlot, boolean useCutOffAng, boolean useBias,
			boolean useIGS, boolean isDual, boolean useGIM, boolean useRTKlib, boolean usePhase, int estimatorType,
			String[] obsvCode, int minSat, String obs_path, String derived_csv_path, String[] obsvCodeList) {
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			HashMap<Integer, ArrayList<IonoValue>> ionoValueMap = new HashMap<Integer, ArrayList<IonoValue>>();

			HashMap<String, ArrayList<HashMap<String, Double>>> ErrMap = new HashMap<String, ArrayList<HashMap<String, Double>>>();

			ArrayList<Calendar> timeList = new ArrayList<Calendar>();
			ArrayList<double[]> trueLLHlist = new ArrayList<double[]>();
			ArrayList<ArrayList<Satellite>> SVlist = new ArrayList<ArrayList<Satellite>>();
			ArrayList<ArrayList<Satellite>[]> dualSVlist = new ArrayList<ArrayList<Satellite>[]>();
			ArrayList<String[]> csvRes = new ArrayList<String[]>();
			Bias bias = null;

			Orbit orbit = null;
			Clock clock = null;
			Antenna antenna = null;
			IONEX ionex = null;
			RTKlib rtkLib = null;

			String GTcsv = "E:\\Study\\Google Decimeter Challenge\\decimeter\\train\\2021-04-29-US-SJC-2\\SamsungS20Ultra\\ground_truth.csv";

			HashMap<String, Object> ObsvMsgComp = ObservationRNX.rinex_obsv_process(obs_path, false, "", obsvCode,
					usePhase);
			@SuppressWarnings("unchecked")
			ArrayList<ObservationMsg> ObsvMsgs = (ArrayList<ObservationMsg>) ObsvMsgComp.get("ObsvMsgs");
			ArrayList<double[]> rxLLH = GroundTruth.processCSV(GTcsv);
			double[][] rxPCO = new double[obsvCode.length][3];
			int interval = (int) ObsvMsgComp.get("interval");
			long week = ObsvMsgs.get(0).getWeekNo();
			int year = Time.getDate(ObsvMsgs.get(0).getTRX(), week, 0).get(Calendar.YEAR);
			int dayNo = Time.getDate(ObsvMsgs.get(0).getTRX(), week, 0).get(Calendar.DAY_OF_YEAR);
			int doW = Time.getDate(ObsvMsgs.get(0).getTRX(), week, 0).get(Calendar.DAY_OF_WEEK) - 1;

			String day = String.format("%03d", dayNo);
			String base_url = "E:\\Study\\Google Decimeter Challenge\\input_files\\";

			String nav_path = base_url + year + "_" + day + "\\BRDC00IGS_R_" + year + day + "0000_01D_MN.rnx";

			String bias_path = base_url + year + "_" + day + "\\CAS0MGXRAP_" + year + day + "0000_01D_01D_DCB.BSX";

			String orbit_path = base_url + year + "_" + day + "\\igs" + week + doW + ".sp3";

			String antenna_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\igs14.atx\\igs14.atx";

			String antenna_csv_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\antenna.csv";

			String clock_path = base_url + year + "_" + day + "\\igs" + week + doW + ".clk_30s";

			int year2dig = year % 2000;
			String ionex_path = base_url + year + "_" + day + "\\igsg" + day + "0." + year2dig + "i";

			String RTKlib_path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\input_files\\complementary\\RTKlib\\decimeter_samsung119.pos";

			if (true) {
				String path = "C:\\Users\\Naman\\Desktop\\rinex_parse_files\\google\\2021-04-29-US-SJC-2\\test";
				File output = new File(path + ".txt");
				PrintStream stream;

				try {
					stream = new PrintStream(output);
					System.setOut(stream);
				} catch (FileNotFoundException e) { // TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			Geoid geoid = buildGeoid();

			int fN = obsvCode.length;
			HashMap<Integer, ArrayList<NavigationMsg>> NavMsgs = null;
			IonoCoeff ionoCoeff = null;
			boolean useDerived = derived_csv_path != null;
			Map<String, Object> NavMsgComp = NavigationRNX.rinex_nav_process(nav_path, useDerived);

			NavMsgs = (HashMap<Integer, ArrayList<NavigationMsg>>) NavMsgComp.getOrDefault("NavMsgs", null);
			ionoCoeff = (IonoCoeff) NavMsgComp.get("ionoCoeff");

			HashMap<Long, HashMap<String, HashMap<Integer, Derived>>> derivedMap = null;
			if (useDerived) {
				derivedMap = DerivedCSV.processCSV(derived_csv_path);

			}

			// Note PVT algos will compute for Antenna Reference Point as it is independent
			// of frequency

			Frame frame = FramesFactory.getITRF(ITRFVersion.ITRF_2014, IERSConventions.IERS_2010, true);

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

				ErrMap.put("RTKlib", new ArrayList<HashMap<String, Double>>());

			}

			for (int i = 0; i < ObsvMsgs.size(); i++) {

				ObservationMsg obsvMsg = ObsvMsgs.get(i);
				double tRX = obsvMsg.getTRX();

				long weekNo = obsvMsg.getWeekNo();
				double[] trueUserLLH = null;
				if (estimatorType != 6) {
					if (Math.round(tRX * 1000) != (rxLLH.get(i)[0] * 1000) || weekNo != rxLLH.get(i)[1]) {

						System.err.println("FATAL ERROR - GT timestamp does not match");
						return null;

					}
					trueUserLLH = new double[] { rxLLH.get(i)[2], rxLLH.get(i)[3], rxLLH.get(i)[4] };
				}

				Calendar time = Time.getDate(tRX, weekNo, 0);
				ArrayList<Satellite> SV = null;
				if (useDerived) {

					SV = com.RINEX_parser.GoogleDecimeter.SingleFreq.process(obsvMsg, ionoCoeff, tRX,
							new double[] { 0, 0, 0 }, false, derivedMap, time, obsvCodeList, weekNo);

				} else {
					SV = SingleFreq.process(obsvMsg, NavMsgs, obsvCode[0], false, false, false, useBias, ionoCoeff,
							bias, orbit, clock, antenna, tRX, weekNo, time, null, new double[] { 0, 0, 0 }, null,
							ionoValueMap, false, null, frame);
				}
				if (SV.size() < minSat) {
					System.err.println("visible satellite count is below threshold");
					continue;
				}
				LS ls = new LS(SV, rxPCO[0], ionoCoeff, time, null, geoid);
				double[] userECEF = null;
				try {
					userECEF = ls.getEstECEF();
				} catch (org.ejml.data.SingularMatrixException e) {
					System.err.println("Matrix Singularity Error occured");
					continue;
				}

				double[] userLatLon = ECEFtoLatLon.ecef2lla(userECEF);
				SV = null;
				ArrayList<Satellite>[] dualSV = null;
				if (useDerived) {
					SV = com.RINEX_parser.GoogleDecimeter.SingleFreq.process(obsvMsg, ionoCoeff, tRX, userECEF,
							useCutOffAng, derivedMap, time, obsvCodeList, weekNo);
					if (SV.size() < minSat) {
						System.err.println("visible satellite count is below threshold");
						continue;
					}

				} else {
					if (isDual) {
						dualSV = DualFreq.process(obsvMsg, NavMsgs, obsvCode, useIGS, useBias, bias, orbit, clock,
								antenna, tRX, weekNo, userECEF, time, useCutOffAng);
						if (dualSV[0].size() < minSat && dualSV[1].size() < minSat) {

							System.err.println("visible satellite count is below threshold");
							continue;
						}
						dualSVlist.add(dualSV);
					} else {
						SV = SingleFreq.process(obsvMsg, NavMsgs, obsvCode[0], useIGS, false, false, useBias, ionoCoeff,
								bias, orbit, clock, antenna, tRX, weekNo, time, null, userECEF, userLatLon,
								ionoValueMap, useCutOffAng, null, frame);
						if (SV.size() < minSat) {
							System.err.println("visible satellite count is below threshold");
							continue;
						}
						SVlist.add(SV);
					}
				}

				switch (estimatorType) {
				case 1:
					ls = null;
					com.RINEX_parser.ComputeUserPos.Regression.DualFreq.LS dualLS = null;
					if (isDual) {
						dualLS = new com.RINEX_parser.ComputeUserPos.Regression.DualFreq.LS(dualSV, rxPCO, userECEF,
								time, geoid);

						ErrMap.computeIfAbsent("dual-LS", k -> new ArrayList<HashMap<String, Double>>())
								.add(estimateError(
										Map.of("IonoCorr", dualLS.getEstECEF(), "TropoCorr", dualLS.getTropoCorrECEF()),
										trueUserLLH, time));

					} else {
						ls = new LS(SV, rxPCO[0], ionoCoeff, time, ionex, geoid);

						ErrMap.computeIfAbsent("LS", k -> new ArrayList<HashMap<String, Double>>())
								.add(estimateError(Map.of("Simple", ls.getEstECEF(), "IonoCorr ", ls.getIonoCorrECEF(),
										"TropoCorr", ls.getTropoCorrECEF()), trueUserLLH, time));
						System.out.println();

					}

					break;
				case 2:
					WLS wls = null;
					com.RINEX_parser.ComputeUserPos.Regression.DualFreq.WLS dualWLS = null;
					if (isDual) {
						dualWLS = new com.RINEX_parser.ComputeUserPos.Regression.DualFreq.WLS(dualSV, rxPCO, userECEF,
								time, geoid);
						ErrMap.computeIfAbsent("dual-WLS", k -> new ArrayList<HashMap<String, Double>>())
								.add(estimateError(Map.of("IonoCorr", dualWLS.getEstECEF(), "TropoCorr",
										dualWLS.getTropoCorrECEF()), trueUserLLH, time));

					} else {
						wls = new WLS(SV, rxPCO[0], ionoCoeff, time, ionex, geoid);
						ErrMap.computeIfAbsent("WLS", k -> new ArrayList<HashMap<String, Double>>())
								.add(estimateError(Map.of("Simple", wls.getEstECEF(), "IonoCorr ",
										wls.getIonoCorrECEF(), "TropoCorr", wls.getTropoCorrECEF()), trueUserLLH,
										time));
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
						ErrMap.computeIfAbsent("dual-LS", k -> new ArrayList<HashMap<String, Double>>())
								.add(estimateError(
										Map.of("IonoCorr", dualLS.getEstECEF(), "TropoCorr", dualLS.getTropoCorrECEF()),
										trueUserLLH, time));
						ErrMap.computeIfAbsent("dual-WLS", k -> new ArrayList<HashMap<String, Double>>())
								.add(estimateError(Map.of("IonoCorr", dualWLS.getEstECEF(), "TropoCorr",
										dualWLS.getTropoCorrECEF()), trueUserLLH, time));

					} else {
						ls = new LS(SV, rxPCO[0], ionoCoeff, time, ionex, geoid);
						wls = new WLS(SV, rxPCO[0], ionoCoeff, time, ionex, geoid);
						ErrMap.computeIfAbsent("LS", k -> new ArrayList<HashMap<String, Double>>())
								.add(estimateError(Map.of("Simple", ls.getEstECEF(), "IonoCorr ", ls.getIonoCorrECEF(),
										"TropoCorr", ls.getTropoCorrECEF()), trueUserLLH, time));

						ErrMap.computeIfAbsent("WLS", k -> new ArrayList<HashMap<String, Double>>())
								.add(estimateError(Map.of("Simple", wls.getEstECEF(), "IonoCorr ",
										wls.getIonoCorrECEF(), "TropoCorr", wls.getTropoCorrECEF()), trueUserLLH,
										time));
					}
					break;
				case 6:
					long millisSinceGpsEpoch = (weekNo * milliSecInWeek) + (Math.round(tRX * 1000));
					wls = new WLS(SV, rxPCO[0], ionoCoeff, time, ionex, geoid);
					double[] estLLH = null;
					try {
						estLLH = ECEFtoLatLon.ecef2lla(wls.getTropoCorrECEF());
					} catch (org.ejml.data.SingularMatrixException e) {
						System.err.println("Matrix Singularity Error occured");
						continue;
					}

					String[] row = new String[] { "", "" + millisSinceGpsEpoch, "" + estLLH[0], "" + estLLH[1] };
					csvRes.add(row);
					break;

				}

				timeList.add(time);
				trueLLHlist.add(trueUserLLH);

			}
			if (estimatorType == 6) {
				return csvRes;
			}

			if (estimatorType == 4) {

//				if (isDual) {
//					if (usePhase) {
//						CycleSlip cs = new CycleSlip(interval);
//						cs.process(dualSVlist);
//					}
//					ErrMap.put("dual-EKF", new ArrayList<HashMap<String, double[]>>());
//					WLS wls = new WLS(dualSVlist.get(0)[0], rxPCO[0], ionoCoeff, timeList.get(0), ionex, geoid);
//					com.RINEX_parser.ComputeUserPos.KalmanFilter.DualFreq.StaticEKF dualStaticEkf = new com.RINEX_parser.ComputeUserPos.KalmanFilter.DualFreq.StaticEKF(
//							dualSVlist, rxPCO, wls.getTropoCorrECEF(), userECEF, geoid, timeList, usePhase);
//					ArrayList<double[]> estECEFList = dualStaticEkf.compute();
//					for (int i = 0; i < dualSVlist.size() - 1; i++) {
//						double[] estECEF = estECEFList.get(i);
//						ErrMap.get("dual-EKF")
//								.add(estimateError(Map.of("TropoCorr", estECEF), userECEF, timeList.get(i)));
//
//					}
//				} else {
				ErrMap.put("EKF", new ArrayList<HashMap<String, Double>>());
				EKF dynamicEkf = new EKF(SVlist, rxPCO[0], null, ionoCoeff, ionex, geoid, timeList);
				ArrayList<double[]> estECEFList = dynamicEkf.computeDynamic(trueLLHlist);
				for (int i = 0; i < SVlist.size() - 1; i++) {

					double[] estECEF = estECEFList.get(i);
					ErrMap.get("EKF")
							.add(estimateError(Map.of("TropoCorr", estECEF), trueLLHlist.get(i), timeList.get(i)));
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

				String posMode = rtkLib.getPosMode();
				ArrayList<Double> rtkTimeList = rtkLib.getTimeList();
				double[] _timeList = rxLLH.stream().mapToDouble(i -> i[0]).toArray();
				for (int i = 0; i < rtkTimeList.size(); i++) {

					double GPStime = rtkTimeList.get(i);

					int key = Arrays.binarySearch(_timeList, GPStime);
					if (key < 0) {
						System.err.println("ERROR in rtklib implementation");
						return null;
					}
					double[] trueUserLLH = new double[] { rxLLH.get(key)[2], rxLLH.get(key)[3], rxLLH.get(key)[4] };
					Calendar time = Time.getDate(rxLLH.get(key)[0], (long) rxLLH.get(key)[1], 0);
					ErrMap.get("RTKlib").add(estimateError(Map.of(posMode, rtkLib.getECEF(i)), trueUserLLH, time));
				}
			}

			HashMap<String, ArrayList<Double>> GraphErrMap = new HashMap<String, ArrayList<Double>>();

			for (String key : ErrMap.keySet()) {

				HashMap<String, ArrayList<Double>> llErrMap = new HashMap<String, ArrayList<Double>>();
				ArrayList<HashMap<String, Double>> errMap = ErrMap.get(key);
				for (HashMap<String, Double> map : errMap) {
					for (String subKey : map.keySet()) {
						double err = map.get(subKey);

						llErrMap.computeIfAbsent(subKey, k -> new ArrayList<Double>()).add(err);
					}
				}
				String header = "order - ";

				String minLL = "LL - ";
				String rmsLL = "LL - ";
				String maeLL = "LL - ";

				for (String subKey : llErrMap.keySet()) {

					ArrayList<Double> llErrList = llErrMap.get(subKey);
					header += subKey + " ";

					minLL += Collections.min(llErrList) + " ";

					rmsLL += RMS(llErrList) + " ";

					maeLL += MAE(llErrList) + " ";

					GraphErrMap.put(key + " " + subKey + " LL off", llErrList);

				}
				System.out.println("\n" + key);
				System.out.println(header);
				System.out.println("MIN - ");
				System.out.println(minLL);
				System.out.println("RMS - ");
				System.out.println(rmsLL);
				System.out.println("MAE - ");
				System.out.println(maeLL);

			}

			if (doPosErrPlot) {

				GraphPlotter chart = new GraphPlotter("GPS PVT Error - ", "Error Estimate", timeList, GraphErrMap);

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
		return null;
	}

	public static HashMap<String, Double> estimateError(Map<String, double[]> ecefMap, double[] userLLH,
			Calendar time) {

		HashMap<String, Double> errMap = new HashMap<String, Double>();

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY hh:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String errStr = sdf.format(time.getTime());
		for (String key : ecefMap.keySet()) {
			double[] estLLH = ECEFtoLatLon.ecef2lla(ecefMap.get(key));

			// Great Circle Distance
			double gcErr = LatLonUtil.getHaversineDistance(estLLH, userLLH);

			errMap.put(key, gcErr);
			errStr += " " + key + " LL diff " + gcErr;

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
