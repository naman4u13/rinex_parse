package com.RINEX_parser.fileParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.stream.IntStream;

import com.RINEX_parser.utility.Time;

public class SBASprint {

	private static final int XIII = Integer.parseInt("111111111111", 2);
	private static final int XI = Integer.parseInt("11111111111", 2);
	private static final int X = Integer.parseInt("1111111111", 2);
	private static final int IX = Integer.parseInt("111111111", 2);
	private static final int VIII = Integer.parseInt("11111111", 2);
	private static HashMap<Integer, Integer> PRNmap = new HashMap<Integer, Integer>();
	private static ArrayList<Integer> PRN = new ArrayList<Integer>();

	public static void sbas_process(String path) {
		File file = new File(path);
		try {
			Scanner input = new Scanner(file);
			while (input.hasNextLine()) {
				String line = input.nextLine();
				String[] msg = line.split("\\s+");
				int n = msg.length;

				int[] tArr = IntStream.range(1, 7).map(x -> Integer.parseInt(msg[x])).toArray();
				long time = Time.getGPSTime(tArr[0], tArr[1] - 1, tArr[2], tArr[3], tArr[4], tArr[5])[0];

				String hex = msg[n - 1];
				String bin = hexToBin(hex);
				int msgType = Integer.parseInt(msg[n - 2]);
				int derived_msgType = Integer.parseInt(bin.substring(8, 14), 2);
				// System.out.println(msgType + " - " + derived_msgType + " - " + bin.length());
				String strData = bin.substring(14, 226);

				System.out.println();
				if (msgType == 1) {
					System.out.println("msgType - " + msgType);
					String[] data = splitter(strData, 210, 2);
					String PRNmask = data[0];
					int IODP = Integer.parseInt(data[1], 2);
					PRN = new ArrayList<Integer>();
					for (int i = 0; i < 210; i++) {
						if (PRNmask.charAt(i) == '1') {
							PRNmap.computeIfAbsent(i, k -> 0);
							PRN.add(i);
						}
					}
					System.out.println("No. of Satellites - " + PRN.size());
					System.out.print("PRN list - ");
					PRNmap.forEach((k, v) -> System.out.print(k + " "));
					System.out.println();
					// System.out.println("IDOP - " + IDOP + " PRN MASK- " + PRNmask2);
				}
				if (msgType == 2 || msgType == 3 || msgType == 4 || msgType == 5) {
					String[] data = splitter(strData, 2, 2, 13 * 12, 13 * 4);
					int IODF = Integer.parseInt(data[0], 2);
					int IODP = Integer.parseInt(data[1], 2);
					String[] strPRC = data[2].split("(?<=\\G.{12})");
					double[] PRC = Arrays.stream(strPRC).mapToDouble(x -> 0.125 * binToDec(x, XIII)).toArray();
					String[] strUDREI = data[3].split("(?<=\\G.{4})");
					int[] UDREI = Arrays.stream(strUDREI).mapToInt(x -> Integer.parseInt(x, 2)).toArray();
					System.out.println("msgType - " + msgType);
					System.out.println("time - " + time);
					System.out.println("IDOP - " + IODP + "  IDOF - " + IODF);
					System.out.print("PRC - ");
					Arrays.stream(PRC).forEach(x -> System.out.print(x + " "));
					System.out.println();
					System.out.print("UDREI - ");
					Arrays.stream(UDREI).forEach(x -> System.out.print(x + " "));
					System.out.println();
				}
				if (msgType == 6) {
					String[] data = splitter(strData, 2, 2, 2, 2, 51 * 4);
					int IODF2 = Integer.parseInt(data[0], 2);
					int IODF3 = Integer.parseInt(data[1], 2);
					int IODF4 = Integer.parseInt(data[2], 2);
					int IODF5 = Integer.parseInt(data[3], 2);
					String[] strUDREI = data[4].split("(?<=\\G.{4})");
					int[] UDREI = Arrays.stream(strUDREI).mapToInt(x -> Integer.parseInt(x, 2)).toArray();
					System.out.println("msgType - " + msgType);
					System.out.println("time - " + time);
					System.out.println(
							"IDOF2 - " + IODF2 + " IDOF3 - " + IODF3 + " IDOF4 - " + IODF4 + " IDOF5 - " + IODF5);
					System.out.print("UDREI - ");
					Arrays.stream(UDREI).forEach(x -> System.out.print(x + " "));
					System.out.println();

				}
				if (msgType == 24) {
					// 102-105 are spare bits
					String[] data = splitter(strData, 106, 106);
					String[] FCdata = splitter(data[0], 12 * 6, 4 * 6, 2, 2, 2, 4);

					String[] strPRC = FCdata[0].split("(?<=\\G.{12})");
					double[] PRC = Arrays.stream(strPRC).mapToDouble(x -> 0.125 * binToDec(x, XIII)).toArray();
					String[] strUDREI = FCdata[1].split("(?<=\\G.{4})");
					int[] UDREI = Arrays.stream(strUDREI).mapToInt(x -> Integer.parseInt(x, 2)).toArray();
					int IODP = Integer.parseInt(FCdata[2], 2);
					int BlockID = Integer.parseInt(FCdata[3], 2);
					int IODF = Integer.parseInt(FCdata[4], 2);
					System.out.println("msgType - " + msgType);
					System.out.println("time - " + time);
					System.out.println("IDOP - " + IODP + "  IDOF - " + IODF + "  Block ID - " + BlockID);
					System.out.print("PRC - ");
					Arrays.stream(PRC).forEach(x -> System.out.print(x + " "));
					System.out.println();
					System.out.print("UDREI - ");
					Arrays.stream(UDREI).forEach(x -> System.out.print(x + " "));
					System.out.println();

					LTCparse(data[1]);

				}
				if (msgType == 7) {
					String[] data = splitter(strData, 4, 2, 2, 204);
					int SysLatency = Integer.parseInt(data[0], 2);
					int IODP = Integer.parseInt(data[1], 2);
					String[] strAi = data[3].split("(?<=\\G.{4})");
					int[] Ai = Arrays.stream(strAi).mapToInt(x -> Integer.parseInt(x, 2)).toArray();
					System.out.println("msgType - " + msgType);
					System.out.println("time - " + time);
					System.out.println("IDOP - " + IODP + "  System Latency - " + SysLatency);
					System.out.print("Degradation factor indicator - ");
					Arrays.stream(Ai).forEach(x -> System.out.print(x + " "));
					System.out.println();

				}
				if (msgType == 25) {
					System.out.println("msgType - " + msgType);
					System.out.println("time - " + time);

					String[] halfMsgs = splitter(strData, 106, 106);
					for (String halfMsg : halfMsgs) {
						LTCparse(halfMsg);
					}
				}

			}

			PRNmap.forEach((k, v) -> System.out.println("PRN - " + k + "  LTC Count -" + v));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e);
			e.printStackTrace();
		}

	}

	private static String hexToBin(String hex) {
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

	private static int binToDec(String bin, int uppBound) {
		int dec = Integer.parseInt(bin, 2);
		if (bin.charAt(0) == '1') {
			dec = uppBound - dec;
			dec += 1;
			dec *= -1;
		}
		return dec;
	}

	private static String[] splitter(String str, int... lens) {

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

	private static void LTCparse(String halfMsg) {

		System.out.println();
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
					if (PRN.size() > 0) {
						int prn = PRN.get(PRNmaskNo);
						PRNmap.put(prn, PRNmap.get(prn) + 1);
					}

					System.out.println("PRNmaskNo - " + PRNmaskNo);
					System.out.println("Velocity Code - " + velCode);
					System.out.println("IODE - " + IODE);
					System.out.println("deltaX - " + deltaX + " deltaY - " + deltaY + " deltaZ - " + deltaZ
							+ " deltaClkOff - " + deltaClkOff);
					System.out.println("IODP - " + IODP);
					System.out.println();
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
				int IODP = Integer.parseInt(data[3], 2);
				if (PRN.size() > 0) {
					int prn = PRN.get(PRNmaskNo);
					PRNmap.put(prn, PRNmap.get(prn) + 1);
				}
				System.out.println("PRNmaskNo - " + PRNmaskNo);
				System.out.println("Velocity Code - " + velCode);
				System.out.println("IODE - " + IODE);
				System.out.println("deltaX - " + deltaX + " deltaY - " + deltaY + " deltaZ - " + deltaZ
						+ " deltaClkOff - " + deltaClkOff);
				System.out.println("deltaXrate - " + deltaXrate + " deltaYrate - " + deltaYrate + " deltaZrate - "
						+ deltaZrate + " deltaClkDrift - " + deltaClkDrift);
				System.out.println("IODE - " + IODE);
				System.out.println("IODP - " + IODP);
				System.out.println("ToA - " + ToA);
			}
		}

	}

}
