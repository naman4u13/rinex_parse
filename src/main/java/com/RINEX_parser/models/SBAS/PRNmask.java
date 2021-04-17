package com.RINEX_parser.models.SBAS;

import java.util.Calendar;
import java.util.HashMap;

public class PRNmask extends SbasRoot {

	private HashMap<Integer, Integer> SVmap;

	public PRNmask(Calendar time, String mask) {
		super(time);
		SVmap = new HashMap<Integer, Integer>();
		map(mask);
		// TODO Auto-generated constructor stub
	}

	private void map(String mask) {
		int index = 0;
		for (int i = 0; i < mask.length(); i++) {
			if (mask.charAt(i) == '1') {
				SVmap.put(i + 1, index);
				index++;
			}

		}
	}

	public HashMap<Integer, Integer> getSVmap() {
		return SVmap;
	}

	public void setSVmap(HashMap<Integer, Integer> sVmap) {
		SVmap = sVmap;
	}

}
