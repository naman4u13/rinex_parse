package com.RINEX_parser.fileParser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.RINEX_parser.utility.StringUtil;

public class Bias {

	private String path;
	private HashMap<Character, HashMap<Integer, HashMap<String, HashMap<String, Double>>>> biasMap;
	private Map<String, Integer> GPSindexMap;
	private HashMap<Integer, double[]> C1Wmap;

	public Bias(String path) throws Exception {
		this.path = path;
		GPSindexMap = Map.of("C1C", 0, "C1W", 1, "C2C", 2, "C2W", 3, "C2S", 4, "C2L", 5, "C2X", 6, "C5Q", 7, "C5X", 8);
		C1Wmap = new HashMap<Integer, double[]>();
		bsx_process();
	}

	private void bsx_process() throws Exception {

		try {
			File file = new File(path);
			HashMap<Integer, double[][]> GPSBiasMat = new HashMap<Integer, double[][]>();
			biasMap = new HashMap<Character, HashMap<Integer, HashMap<String, HashMap<String, Double>>>>();
			Scanner input = new Scanner(file);
			input.useDelimiter("\\+BIAS/SOLUTION|\\-BIAS/SOLUTION");
			input.next();
			Scanner biasSoln = new Scanner(input.next());
			biasSoln.useDelimiter("\n");
			biasSoln.next();
			String[] fields = biasSoln.next().split("\\s+");
			while (biasSoln.hasNext()) {
				String line = biasSoln.next();
				int len = line.length();
				String[] soln;
				if (len == 137) {
					soln = StringUtil.splitter(line, 5, 5, 4, 10, 5, 5, 15, 15, 5, 22, 12, 22, 12);
				} else {
					soln = StringUtil.splitter(line, 5, 5, 4, 10, 5, 5, 15, 15, 5, 22, 12);
				}

				if (soln[0].equals("DSB")) {
					if (soln[3].isBlank()) {
						String PRNstr = soln[2];
						char SSI = PRNstr.charAt(0);
						int prn = Integer.parseInt(PRNstr.substring(1));
						String obs1 = soln[4];
						String obs2 = soln[5];
						double biasValue = Double.parseDouble(soln[9]) * 1e-9;
						biasMap.computeIfAbsent(SSI,
								k -> new HashMap<Integer, HashMap<String, HashMap<String, Double>>>())
								.computeIfAbsent(prn, k -> new HashMap<String, HashMap<String, Double>>())
								.computeIfAbsent(obs1, k -> new HashMap<String, Double>()).put(obs2, biasValue);
						if (SSI == 'G') {
							int i = GPSindexMap.get(obs1);
							int j = GPSindexMap.get(obs2);

							double[][] mat = GPSBiasMat.computeIfAbsent(prn, k -> new double[9][9]);
							mat[i][j] = biasValue;
							mat[j][i] = -biasValue;

						}

					}
				}
			}
			computeAllGPSBias(GPSBiasMat);

		} catch (Exception e) {
			throw new Exception("Error occured during parsing of Bias(.BSX) file \n" + e);

		}

	}

	public void computeAllGPSBias(HashMap<Integer, double[][]> GPSBiasMat) {

		for (int prn : GPSBiasMat.keySet()) {
			double[] C1W = new double[9];
			double[][] mat = GPSBiasMat.get(prn);
			C1W[0] = mat[1][0];
			C1W[1] = 0;
			C1W[2] = mat[0][3] + mat[3][2];
			C1W[3] = mat[1][3];
			C1W[4] = mat[0][3] + mat[3][4];
			C1W[5] = mat[0][3] + mat[3][5];
			C1W[6] = mat[0][3] + mat[3][6];
			C1W[7] = mat[0][1] + mat[1][7];
			C1W[8] = mat[0][1] + mat[1][8];
			C1Wmap.put(prn, C1W);
		}

	}

	public double getISC(String obsvCode, int PRN) {
		char SSI = obsvCode.charAt(0);
		// Observable/Observation code in RINEX format
		String _obsvCode = 'C' + obsvCode.substring(1);
		if (SSI == 'G') {
			int index = GPSindexMap.get(_obsvCode);
			double ISC = C1Wmap.get(PRN)[index];
			return ISC;
		}
		HashMap<String, HashMap<String, Double>> _biasMap = biasMap.get(SSI).get(PRN);
		double ISC = 0;
		if (_biasMap.containsKey("C1W") && _biasMap.get("C1W").containsKey(_obsvCode)) {

			ISC = _biasMap.get("C1W").get(_obsvCode);
		} else {
			ISC = -_biasMap.get(_obsvCode).get("C1W");
		}

		return ISC;
	}
}
