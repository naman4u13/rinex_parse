package com.RINEX_parser.fileParser;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

public class Bias {

	public static HashMap<Integer, Double> bsx_process(String path) {
		File file = new File(path);
		try {
			Scanner input = new Scanner(file);
			input.useDelimiter("\\+BIAS/SOLUTION|\\-BIAS/SOLUTION");
			input.next();
			Scanner bias = new Scanner(input.next());
			bias.useDelimiter("\n");

			String[] fields = bias.next().split("\\s+");

		} catch (Exception e) {
			// TODO: handle exception
		}

		return null;
	}
}
