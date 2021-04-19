package com.RINEX_parser.fileParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

import com.RINEX_parser.models.SBAS.Correction;
import com.RINEX_parser.models.SBAS.FastCorr;
import com.RINEX_parser.models.SBAS.LongTermCorr;
import com.RINEX_parser.utility.Time;

public class SBAS {

	private final int XII = Integer.parseInt("111111111111", 2);
	private final int XI = Integer.parseInt("11111111111", 2);
	private final int X = Integer.parseInt("1111111111", 2);
	private final int IX = Integer.parseInt("111111111", 2);
	private final int VIII = Integer.parseInt("11111111", 2);

	private String[] lines;
	private int lineCtr;
	private HashMap<Integer, Correction> PRNmap = null;
	private int currentIODP = -999;
	private ArrayList<Integer> PRNmask;
	// private
	private int PRNsize;

	public SBAS(String path) {

		try {
			Path fileName = Path.of(path);
			String input = Files.readString(fileName);
			this.lines = input.split("\r\n|\r|\n");
			this.lineCtr = 0;
		} catch (Exception e) {
			System.err.println(e);
		}

	}

	public void process(long timeBound) {

		while (lineCtr < lines.length) {
			String line = lines[lineCtr];
			String[] msg = line.split("\\s+");
			int n = msg.length;
			int[] tArr = IntStream.range(1, 7).map(x -> Integer.parseInt(msg[x])).toArray();

			long[] time = Time.getGPSTime(tArr[0] + 2000, tArr[1] - 1, tArr[2], tArr[3], tArr[4], tArr[5]);
			long GPSTime = time[0];
			long weekNo = time[1];
			if (GPSTime > timeBound) {
				return;
			}
			String hex = msg[n - 1];
			String bin = hexToBin(hex);
			int msgType = Integer.parseInt(msg[n - 2]);
			int derived_msgType = Integer.parseInt(bin.substring(8, 14), 2);
			String strData = bin.substring(14, 226);

			if (msgType == 1) {
				String[] data = splitter(strData, 210, 2);
				String mask = data[0];
				int IODP = Integer.parseInt(data[1], 2);
				if (currentIODP != IODP) {
					PRNmap = new HashMap<Integer, Correction>();
					currentIODP = IODP;
					PRNmask = new ArrayList<Integer>();
					for (int i = 1; i <= 210; i++) {
						if (mask.charAt(i - 1) == '1') {

							PRNmap.put(i, new Correction());
							PRNmask.add(i);
						}
					}
					PRNsize = PRNmask.size();
				}
				// System.out.println();
			} else if (PRNmap != null) {

				if (msgType == 2 || msgType == 3 || msgType == 4 || msgType == 5) {
					String[] data = splitter(strData, 2, 2, 13 * 12, 13 * 4);
					int IODF = Integer.parseInt(data[0], 2);
					int IODP = Integer.parseInt(data[1], 2);
					String[] strPRC = data[2].split("(?<=\\G.{12})");
					double[] PRC = Arrays.stream(strPRC).mapToDouble(x -> 0.125 * binToDec(x, XII)).toArray();
					String[] strUDREI = data[3].split("(?<=\\G.{4})");
					int[] UDREI = Arrays.stream(strUDREI).mapToInt(x -> Integer.parseInt(x, 2)).toArray();

					int index = 13 * (msgType - 2);
					int N = PRNsize - index < 13 ? PRNsize - index : 13;
					for (int i = 0; i < N; i++) {
						FastCorr fc = new FastCorr(GPSTime, weekNo, PRC[i], IODP, IODF, UDREI[i]);
						int prn = PRNmask.get(index + i);
						PRNmap.get(prn).setFC(fc);
					}
					// System.out.println();
				} else if (msgType == 24) {
					// 102-105 are spare bits
					String[] data = splitter(strData, 106, 106);
					String[] FCdata = splitter(data[0], 12 * 6, 4 * 6, 2, 2, 2, 4);

					String[] strPRC = FCdata[0].split("(?<=\\G.{12})");
					double[] PRC = Arrays.stream(strPRC).mapToDouble(x -> 0.125 * binToDec(x, XII)).toArray();
					String[] strUDREI = FCdata[1].split("(?<=\\G.{4})");
					int[] UDREI = Arrays.stream(strUDREI).mapToInt(x -> Integer.parseInt(x, 2)).toArray();
					int IODP = Integer.parseInt(FCdata[2], 2);
					int BlockID = Integer.parseInt(FCdata[3], 2);
					int IODF = Integer.parseInt(FCdata[4], 2);

					int index = 13 * BlockID;
					int N = PRNsize - index;
					for (int i = 0; i < N; i++) {
						FastCorr fc = new FastCorr(GPSTime, weekNo, PRC[i], IODP, IODF, UDREI[i]);
						int prn = PRNmask.get(index + i);
						PRNmap.get(prn).setFC(fc);
					}

					LTCparse(data[1], GPSTime, weekNo);

				}
//				if (msgType == 7) {
//					String[] data = splitter(strData, 4, 2, 2, 204);
//					int SysLatency = Integer.parseInt(data[0], 2);
//					int IODP = Integer.parseInt(data[1], 2);
//					String[] strAi = data[3].split("(?<=\\G.{4})");
//					int[] Ai = Arrays.stream(strAi).mapToInt(x -> Integer.parseInt(x, 2)).toArray();
//					
//
//				}
				if (msgType == 25) {

					String[] halfMsgs = splitter(strData, 106, 106);
					for (String halfMsg : halfMsgs) {
						LTCparse(halfMsg, GPSTime, weekNo);
					}
				}
			}
			lineCtr++;

		}

	}

	private String hexToBin(String hex) {
		hex = hex.replaceAll("0", "0000");
		hex = hex.replaceAll("1", "0001");
		hex = hex.replaceAll("2", "0010");
		hex = hex.replaceAll("3", "0011");
		hex = hex.replaceAll("4", "0100");
		hex = hex.replaceAll("5", "0101");
		hex = hex.replaceAll("6", "0110");
		hex = hex.replaceAll("7", "0111");
		hex = hex.replaceAll("8", "1000");
		hex = hex.replaceAll("9", "1001");
		hex = hex.replaceAll("A", "1010");
		hex = hex.replaceAll("B", "1011");
		hex = hex.replaceAll("C", "1100");
		hex = hex.replaceAll("D", "1101");
		hex = hex.replaceAll("E", "1110");
		hex = hex.replaceAll("F", "1111");
		return hex;
	}

	private int binToDec(String bin, int uppBound) {
		int dec = Integer.parseInt(bin, 2);
		if (bin.charAt(0) == '1') {
			dec = uppBound - dec;
			dec += 1;
			dec *= -1;
		}
		return dec;
	}

	private String[] splitter(String str, int... lens) {

		if (Arrays.stream(lens).sum() != str.length()) {
			System.out.println("ERROR: splitter args are incorrect");
			return null;
		}
		int pos = 0;
		int n = lens.length;
		String[] arr = new String[n];
		for (int i = 0; i < n; i++) {
			arr[i] = str.substring(pos, pos + lens[i]);
			pos += lens[i];
		}
		return arr;

	}

	private void LTCparse(String halfMsg, long GPSTime, long weekNo) {

		if (halfMsg.charAt(0) == '0') {
			String[] data = splitter(halfMsg, 1, 51, 51, 2, 1);
			int velCode = Integer.parseInt(data[0]);
			String[] qtrMsgs = new String[] { data[1], data[2] };
			for (String qtrMsg : qtrMsgs) {
				String[] params = splitter(qtrMsg, 6, 8, 9, 9, 9, 10);
				int PRNmaskNo = Integer.parseInt(params[0], 2);
				if (PRNmaskNo != 0) {
					int IODE = Integer.parseInt(params[1], 2);
					double deltaX = 0.125 * binToDec(params[2], IX);
					double deltaY = 0.125 * binToDec(params[3], IX);
					double deltaZ = 0.125 * binToDec(params[4], IX);
					double deltaClkOff = Math.pow(2, -31) * binToDec(params[5], X);
					int IODP = Integer.parseInt(data[3], 2);
					int prn = PRNmask.get(PRNmaskNo - 1);
					LongTermCorr ltc = new LongTermCorr(GPSTime, weekNo, velCode,
							new double[] { deltaX, deltaY, deltaZ, deltaClkOff });
					PRNmap.get(prn).setLTC(IODE, ltc);
					// System.out.println();
				}
			}

		} else {
			String[] data = splitter(halfMsg, 1, 6, 8, 11, 11, 11, 11, 8, 8, 8, 8, 13, 2);
			int velCode = Integer.parseInt(data[0]);
			int PRNmaskNo = Integer.parseInt(data[1], 2);
			if (PRNmaskNo != 0) {
				int IODE = Integer.parseInt(data[2], 2);
				double deltaX = 0.125 * binToDec(data[3], XI);
				double deltaY = 0.125 * binToDec(data[4], XI);
				double deltaZ = 0.125 * binToDec(data[5], XI);
				double deltaClkOff = Math.pow(2, -31) * binToDec(data[6], XI);
				double deltaXrate = Math.pow(2, -11) * binToDec(data[7], VIII);
				double deltaYrate = Math.pow(2, -11) * binToDec(data[8], VIII);
				double deltaZrate = Math.pow(2, -11) * binToDec(data[9], VIII);
				double deltaClkDrift = Math.pow(2, -39) * binToDec(data[10], VIII);
				long ToA = 16 * Long.parseLong(data[11], 2);

				long modGPSTime = GPSTime - (GPSTime % 86400) + ToA;

				int IODP = Integer.parseInt(data[3], 2);
				int prn = PRNmask.get(PRNmaskNo - 1);
				LongTermCorr ltc = new LongTermCorr(modGPSTime, weekNo, velCode,
						new double[] { deltaX, deltaY, deltaZ, deltaClkOff },
						new double[] { deltaXrate, deltaYrate, deltaZrate, deltaClkDrift });
				PRNmap.get(prn).setLTC(IODE, ltc);
				// System.out.println();

			}
		}

	}

	public HashMap<Integer, Correction> getPRNmap() {
		return PRNmap;
	}

}
