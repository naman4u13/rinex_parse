package com.RINEX_parser.utility;

import java.util.ArrayList;
import java.util.Collections;

public class Closest {
	public static int findClosest_BS(long val, ArrayList<Long> list) {
		int min_index = Collections.binarySearch(list, val);
		// if the val exist in the list
		if (min_index >= 0) {
			return min_index;
		}
		// find insertion point
		min_index = -min_index - 1;
		// if element is the smallest
		if (min_index == 0) {
			return min_index;
		}
		// Check if its not the larger than whole list
		if (min_index < list.size()) {
			min_index = ((list.get(min_index) - val) < (list.get(min_index - 1) - val)) ? min_index : min_index - 1;
			return min_index;
		}
		// Return the last element as it is the closest
		return min_index - 1;

	}

	public static int findClosest(long val, ArrayList<Long> list) {
		long min = Long.MAX_VALUE;
		int min_index = 0;
		for (int i = 0; i < list.size(); i++) {
			long diff = Math.abs(list.get(i) - val);
			if (diff < min) {
				min = diff;
				min_index = i;
			}

		}
		return min_index;
	}
}
