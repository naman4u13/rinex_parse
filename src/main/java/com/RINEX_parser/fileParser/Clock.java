package com.RINEX_parser.fileParser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.RINEX_parser.models.IGS.IGSClock;
import com.RINEX_parser.utility.Interpolator;
import com.RINEX_parser.utility.StringUtil;
import com.RINEX_parser.utility.Time;

public class Clock {

	private ArrayList<IGSClock> IGSClockList;
	private int[] pts;
	private Bias bias;
	private final static double GPS_FREQ_RATIO = ((154.0 * 154.0) / (120.0 * 120.0));

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
			int satCount = 0;
			for (String line : header) {

				String[] cols = StringUtil.splitter(line, 60, 20);
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
					String[] strTime = StringUtil.splitter(data[i], false, 8, 26)[1].split("\\s+");
					long[] time = Time.getGPSTime(strTime);
					long GPSTime = time[0];
					long weekNo = time[1];
					HashMap<Integer, Double> biasMap = new HashMap<Integer, Double>();
					for (int j = 0; j < satCount; j++) {
						String[] clkData = StringUtil.splitter(data[i], false, 3, 5, 32, 19);
						String SVName = clkData[1];
						char SSI = SVName.charAt(0);
						int SVID = Integer.parseInt(SVName.substring(1));
						double clkBias = Double.parseDouble(clkData[3]);
						biasMap.put(SVID, clkBias);
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

	public double getBias(double x, int SVID, String obsvCode) {

		return getBias(x, SVID, new String[] { obsvCode })[0];

	}

	public double[] getBias(double x, int SVID, String[] obsvCode) {
		double[] X = new double[2];
		double[] Y = new double[2];

		for (int i = 0; i < 2; i++) {
			IGSClock clk = IGSClockList.get(pts[i]);
			X[i] = clk.getTime();
			Y[i] = clk.getClkBias().get(SVID);

		}

		double clkBias = Interpolator.linear(X, Y, x);

		double TGD = bias.getISC("G2W", SVID) / (1 - GPS_FREQ_RATIO);
		int fN = obsvCode.length;
		double[] clkBiases = new double[fN];
		for (int i = 0; i < fN; i++) {
			double ISC = bias.getISC(obsvCode[i], SVID);
			clkBiases[i] = clkBias - TGD + ISC;
		}

		return clkBiases;

	}

}
