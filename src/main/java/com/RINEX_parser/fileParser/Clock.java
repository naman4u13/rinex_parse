package com.RINEX_parser.fileParser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.RINEX_parser.constants.Constellation;
import com.RINEX_parser.models.IGS.IGSClock;
import com.RINEX_parser.utility.Interpolator;
import com.RINEX_parser.utility.StringUtil;
import com.RINEX_parser.utility.Time;

public class Clock {

	private ArrayList<IGSClock> IGSClockList;
	private int[] pts;
	private Bias bias;

	public Clock(String path, Bias bias) throws Exception {
		IGSClockList = new ArrayList<IGSClock>();
		clock_process(path);
		this.bias = bias;

	}

	private void clock_process(String path) throws Exception {

		try {

			File file = new File(path);
			Scanner input = new Scanner(file);
			input.useDelimiter("END OF HEADER");
			String[] header = input.next().split("\r\n|\r|\n");
			int recLen = 60;
			int[] recFormat = new int[] { 3, 5, 26, 6, 19 };

			if (header[0].split("\\s")[0].trim().equals("3.04")) {
				recLen = 65;
				recFormat = new int[] { 3, 10, 27, 5, 19 };
			}
			int satCount = 0;
			for (String line : header) {

				String[] cols = StringUtil.splitter(line, recLen, 20);
				String label = cols[1];
				if (label.equalsIgnoreCase("# OF SOLN SATS")) {
					satCount = Integer.parseInt(cols[0]);
					break;
				}

			}
			String[] data = input.next().split("\r\n|\r|\n");
			for (int i = 0; i < data.length; i++) {
				String clkType = StringUtil.splitter(data[i], false, 3)[0];
				if (clkType.equalsIgnoreCase("AS")) {
					String[] strTime = StringUtil.splitter(data[i], false, recFormat[0] + recFormat[1], recFormat[2])[1]
							.split("\\s+");
					double[] time = Time.getGPSTime(strTime);
					double GPSTime = time[0];
					long weekNo = (long) time[1];
					HashMap<Character, HashMap<Integer, Double>> biasMap = new HashMap<Character, HashMap<Integer, Double>>();

					while (i < data.length) {
						clkType = StringUtil.splitter(data[i], false, 3)[0];
						if (clkType.equalsIgnoreCase("AR")) {
							i++;
							continue;
						}
						String[] cStrTime = StringUtil.splitter(data[i], false, recFormat[0] + recFormat[1],
								recFormat[2])[1].split("\\s+");
						boolean flag = false;
						for (int k = strTime.length - 1; k > 1; k--) {
							if (!cStrTime[k].equals(strTime[k])) {
								flag = true;
								break;
							}
						}
						if (flag) {
							i--;
							break;
						}
						String[] clkData = StringUtil.splitter(data[i], false, recFormat[0], recFormat[1],
								recFormat[2] + recFormat[3], recFormat[4]);
						String SVName = clkData[1];
						char SSI = SVName.charAt(0);

						int SVID = Integer.parseInt(SVName.substring(1));
						double clkBias = Double.parseDouble(clkData[3]);
						biasMap.computeIfAbsent(SSI, k -> new HashMap<Integer, Double>()).put(SVID, clkBias);

						i++;

					}
					IGSClockList.add(new IGSClock(GPSTime, biasMap));

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Error occured during parsing of IGS Clock(.clk) file \n" + e);
			// TODO: handle exception
		}
	}

	public void findPts(double x) {

		// Boundary Index
		pts = null;
		int n = IGSClockList.size();
		for (int i = 0; i < n - 1; i++) {
			double x1 = IGSClockList.get(i).getTime();
			double x2 = IGSClockList.get(i + 1).getTime();
			if (x >= x1 && x < x2) {
				pts = new int[] { i, i + 1 };
				return;
			}
		}
		if (x == IGSClockList.get(n - 1).getTime()) {
			pts = new int[] { n - 2, n - 1 };

		}

	}

	public double getBias(double x, int SVID, String obsvCode, boolean applyDCB) {

		return getBias(x, SVID, new String[] { obsvCode }, applyDCB)[0];

	}

	@SuppressWarnings("unused")
	public double[] getBias(double x, int SVID, String[] obsvCode, boolean applyDCB) {
		double[] X = new double[2];
		double[] Y = new double[2];
		char SSI = obsvCode[0].charAt(0);
		for (int i = 0; i < 2; i++) {

			IGSClock clk = IGSClockList.get(pts[i]);
			X[i] = clk.getTime();
			try {
				Y[i] = clk.getClkBias().get(SSI).get(SVID);
			} catch (Exception e) {
				System.out.println();
			}

		}

		double clkBias = Interpolator.linear(X, Y, x);
		int fN = obsvCode.length;
		double[] clkBiases = new double[fN];
		if (applyDCB) {
			if (SSI == 'G') {
				double gpsFreqRatio = Math.pow(Constellation.frequency.get('G').get(1), 2)
						/ Math.pow(Constellation.frequency.get('G').get(2), 2);

				double TGD = bias.getISC("G2W", SVID) / (1 - gpsFreqRatio);

				for (int i = 0; i < fN; i++) {
					double ISC = bias.getISC(obsvCode[i], SVID);
					clkBiases[i] = clkBias - TGD + ISC;
				}
			} else if (SSI == 'E') {
				double galileoFreqRatio = Math.pow(Constellation.frequency.get('E').get(1), 2)
						/ Math.pow(Constellation.frequency.get('E').get(5), 2);
				for (int i = 0; i < fN; i++) {

					if (obsvCode[i] == "E1C") {
						double BGD = bias.getISC("E5Q", SVID) / (1 - galileoFreqRatio);
						clkBiases[i] = clkBias - BGD;

					} else if (obsvCode[i] == "E5X") {
						double BGD = bias.getISC("E5X", SVID) / (1 - galileoFreqRatio);
						clkBiases[i] = clkBias - (galileoFreqRatio * BGD);
					}
				}
			} else if (SSI == 'C') {

				for (int i = 0; i < fN; i++) {
					Double ISC = bias.getISC("C2I", SVID);
					clkBiases[i] = clkBias + ISC;
				}

			}
		}
		return clkBiases;

	}

}
