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

	public static double[][] str2mArr_D(String str) {
		return str2mArr_D(str, 1);
	}

	public static double[][] str2mArr_D(String str, double scale) {
		str = str.substring(2, str.length() - 2);
		String[] strArr = str.split("\\]\\s*,\\s*\\[");
		int n = strArr.length;
		double[][] mArr = new double[n][];
		for (int i = 0; i < n; i++) {
			mArr[i] = Arrays.stream(strArr[i].split(",")).map(j -> j.trim())
					.mapToDouble(j -> Double.parseDouble(j) * scale).toArray();
		}
		return mArr;
	}

	public static double[] str2arr_D(String str) {
		return str2arr_D(str, 1);
	}

	public static double[] str2arr_D(String str, double scale) {
		str = str.substring(1, str.length() - 1);
		double[] arr = Arrays.stream(str.split(",")).map(i -> i.trim()).mapToDouble(i -> Double.parseDouble(i) * scale)
				.toArray();
		return arr;
	}

	public static long[] str2arr_L(String str) {
		str = str.substring(1, str.length() - 1);
		long[] arr = Arrays.stream(str.split(",")).map(i -> i.trim()).mapToLong(Long::parseLong).toArray();
		return arr;
	}

}
