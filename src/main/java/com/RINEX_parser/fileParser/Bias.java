package com.RINEX_parser.fileParser;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

import com.RINEX_parser.utility.StringUtil;

public class Bias {

	private String path;
	private HashMap<Character, HashMap<Integer, HashMap<String, HashMap<String, Double>>>> biasMap;

	public Bias(String path) throws Exception {
		this.path = path;
		bsx_process();
	}

	private void bsx_process() throws Exception {

		try {
			File file = new File(path);
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

					}
				}
			}

		} catch (Exception e) {
			throw new Exception("Error occured during parsing of Bias(.BSX) file \n" + e);

		}

	}

	public double getDCB(String obsvCode, int PRN) {
		char SSI = obsvCode.charAt(0);
		// Observable/Observation code in RINEX format
		String _obsvCode = 'C' + obsvCode.substring(1);
		double DCB = biasMap.get(SSI).get(PRN).get(_obsvCode).get("C1W");
		return DCB;
	}
}
