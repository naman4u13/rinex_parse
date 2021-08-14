package com.RINEX_parser.utility;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SymbolToken {

	public static ArrayList<String> split(String token) {
		ArrayList<String> list = new ArrayList<String>();
		String pattern = "[0-9](-)";
		Pattern r = Pattern.compile(pattern);
		int start = 0;
		Matcher m = r.matcher(token);
		while (m.find()) {
			list.add(token.substring(start, m.start() + 1));

			start = m.start() + 1;
		}
		list.add(token.substring(start, token.length()));
		return list;

	}
}
