package com.RINEX_parser.utility;

import java.util.Arrays;

public class StringUtil {

	public static String[] splitter(String str, int... lens) {

		if (Arrays.stream(lens).sum() != str.length()) {
			System.out.println("ERROR: splitter args are incorrect");
			return null;
		}
		int pos = 0;
		int n = lens.length;
		String[] arr = new String[n];
		for (int i = 0; i < n; i++) {
			arr[i] = str.substring(pos, pos + lens[i]).trim();
			pos += lens[i];
		}
		return arr;

	}
}
