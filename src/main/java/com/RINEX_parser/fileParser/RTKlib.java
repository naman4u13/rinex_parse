package com.RINEX_parser.fileParser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.stream.IntStream;

import com.RINEX_parser.utility.Time;

public class RTKlib {

	private ArrayList<double[]> ecefList;
	private ArrayList<Double> timeList;

	private String posMode = "";

	public RTKlib(String path) throws Exception {
		ecefList = new ArrayList<double[]>();
		timeList = new ArrayList<Double>();
		process(path);
	}

	public void process(String path) throws Exception {

		try {

			File file = new File(path);
			Scanner input = new Scanner(file);
			input.useDelimiter("%\r\n");
			String header = input.next();
			String body = input.next();

			posMode = header.split("% pos mode  :")[1].split("\\n")[0].trim();

			String[] lines = body.split("\\n");
			int len = lines.length;
			lines = Arrays.copyOfRange(lines, 2, len);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			Calendar cal = Calendar.getInstance();
			for (String rec : lines) {
				String[] data = rec.split("\\s+");
				String[] strDate = (data[0].trim() + " " + data[1].trim()).split("\\.");

				double[] ecef = IntStream.range(2, 5).mapToDouble(i -> Double.parseDouble(data[i])).toArray();
				ecefList.add(ecef);
				Date date = sdf.parse(strDate[0]);
				cal.setTime(date);
				Double GPSTime = Time.getGPSTime(cal)[0];
				GPSTime += Double.parseDouble(strDate[1]) * Math.pow(10, -strDate[1].length());
				timeList.add(GPSTime);

			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Error occured during parsing of RTKlib file(.pos) file \n" + e);
			// TODO: handle exception
		}

	}

	public int getIndex(double GPSTime) {
		int index = timeList.indexOf(GPSTime);
		return index;
	}

	public double[] getECEF(int i) {

		return ecefList.get(i);
	}

	public String getPosMode() {
		return posMode;
	}

	public double getTime(int i) {
		return timeList.get(i);
	}

	public ArrayList<Double> getTimeList() {
		return timeList;
	}
}
