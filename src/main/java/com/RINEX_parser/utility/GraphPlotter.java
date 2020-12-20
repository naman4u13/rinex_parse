package com.RINEX_parser.utility;

import java.util.ArrayList;
import java.util.HashMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.RINEX_parser.models.IonoValue;

public class GraphPlotter extends ApplicationFrame {

	public GraphPlotter(String applicationTitle, String chartTitle, HashMap<Integer, ArrayList<IonoValue>> data) {
		super(applicationTitle);
		// TODO Auto-generated constructor stub

		final JFreeChart chart = ChartFactory.createTimeSeriesChart(chartTitle, "Time of the Day", "Iono Corr Value",
				createDataset(data), true, true, false);

		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 370));
		chartPanel.setMouseZoomable(true, false);

		setContentPane(chartPanel);

	}

	private TimePeriodValuesCollection createDataset(HashMap<Integer, ArrayList<IonoValue>> data) {
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

	private TimeSeriesCollection createDataset2(ArrayList<IonoValue> data) {
		final TimeSeries series = new TimeSeries("GPS DATA");
		for (IonoValue value : data) {
			System.out.println(
					value.getSVID() + "  " + value.getTime().toString() + "  iono corr " + value.getIonoCorr());

			series.add(new Second(value.getTime()), value.getIonoCorr());
		}

		return new TimeSeriesCollection(series);
	}

}
