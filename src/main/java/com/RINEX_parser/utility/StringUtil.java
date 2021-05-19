package com.RINEX_parser.utility;

import java.util.Arrays;

public class StringUtil {

	public static String[] splitter(String str, int... lens) {

		return splitter(str, true, lens);

	}

	public static String[] splitter(String str, boolean flag, int... lens) {

		int n = lens.length;
		int lenSum = Arrays.stream(lens).sum();
		if (lenSum != str.length()) {
			if (flag) {
				System.out.println("ERROR: splitter args are incorrect");
				return null;
			} else {
				int lastEle = str.length() - lenSum;
				int[] temp = lens;
				n += 1;
				lens = new int[n];
				System.arraycopy(temp, 0, lens, 0, n - 1);
				lens[n - 1] = lastEle;
			}

		}
		int pos = 0;

		String[] arr = new String[n];
		for (int i = 0; i < n; i++) {
			arr[i] = str.substring(pos, pos + lens[i]).trim();
			pos += lens[i];
		}
		return arr;

	}

}
