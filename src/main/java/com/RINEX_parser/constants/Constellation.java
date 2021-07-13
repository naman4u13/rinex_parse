package com.RINEX_parser.constants;

import java.util.Map;

public class Constellation {

//	public enum constellation {
//		GPS, GLONASS, GALILEO, BEIDOU, QZSS, IRNSS;
//	}
//
//	public static Map<Character, constellation> id = Map.of('G', constellation.GPS, 'R', constellation.GLONASS, 'E',
//			constellation.GALILEO, 'C', constellation.BEIDOU, 'J', constellation.QZSS, 'I', constellation.IRNSS);

	public static Map<Character, Map<Integer, Double>> frequency = Map.of('G',
			Map.of(1, 1575.42e6, 2, 1227.60e6, 5, 1176.45e6), 'R',
			Map.of(1, 1602e6, 2, 1246e6, 3, 1227.60e6, 4, 1600.995e6, 6, 1248.06e6), 'E',
			Map.of(1, 1575.42e6, 5, 1176.45e6, 7, 1207.140e6, 8, 1191.795e6, 6, 1278.75e6), 'J',
			Map.of(1, 1575.42e6, 2, 1227.60e6, 5, 1176.45e6, 6, 1278.75e6), 'C',
			Map.of(1, 1575.42e6, 2, 1561.098e6, 5, 1176.45e6, 6, 1268.52e6, 7, 1207.14e6, 8, 1191.795e6), 'I',
			Map.of(5, 1176.45e6, 9, 2492.028e6), 'S', Map.of(1, 1575.42e6, 5, 1176.45e6)

	);
	// 1 - 1.57542003E9
	// 5 - 1.17645005E9

}
