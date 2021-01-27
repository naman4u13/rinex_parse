package com.RINEX_parser.utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;

import com.RINEX_parser.models.IonoValue;

public class GraphPlotter extends ApplicationFrame {

	public GraphPlotter(String applicationTitle, String chartTitle, HashMap<Integer, ArrayList<IonoValue>> data) {
		super(applicationTitle);
		// TODO Auto-generated constructor stub

		final JFreeChart chart = ChartFactory.createTimeSeriesChart(chartTitle, "Time of the Day", "Iono Corr Value",
				createDatasetIono(data), true, true, false);

		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 370));
		chartPanel.setMouseZoomable(true, false);

		setContentPane(chartPanel);

	}

	public GraphPlotter(String applicationTitle, String chartTitle, ArrayList<Calendar> timeList,
			HashMap<String, ArrayList<Double>> ErrMap) {
		super(applicationTitle);
		// TODO Auto-generated constructor stub

		final JFreeChart chart = ChartFactory.createTimeSeriesChart(chartTitle, "Time of the Day", "Position Error",
				createDatasetError(timeList, ErrMap), true, true, false);

		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 370));
		chartPanel.setMouseZoomable(true, false);

		setContentPane(chartPanel);

	}

	private TimePeriodValuesCollection createDatasetIono(HashMap<Integer, ArrayList<IonoValue>> data) {
		TimePeriodValuesCollection coll = new TimePeriodValuesCollection();
		for (int SVID : data.keySet()) {
			TimePeriodValues series = new TimePeriodValues(String.valueOf(SVID));
			ArrayList<IonoValue> list = data.get(SVID);
			for (IonoValue value : list) {
				System.out.println(SVID + "  " + value.getTime().toString() + "  iono corr " + value.getIonoCorr());

				series.add(new Second(value.getTime()), value.getIonoCorr());
			}
			System.out.println("");
			coll.addSeries(series);
		}
		return coll;

	}

	private TimePeriodValuesCollection createDatasetError(ArrayList<Calendar> timeList,
			HashMap<String, ArrayList<Double>> ErrMap) {
		TimePeriodValuesCollection coll = new TimePeriodValuesCollection();
		for (String ErrType : ErrMap.keySet()) {
			TimePeriodValues series = new TimePeriodValues(ErrType);
			ArrayList<Double> ErrList = ErrMap.get(ErrType);
			for (int i = 0; i < ErrList.size(); i++) {

				series.add(new Second(timeList.get(i).getTime()), ErrList.get(i));
			}

			coll.addSeries(series);
		}
		return coll;
	}
}
