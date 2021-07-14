package com.RINEX_parser.utility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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

	public GraphPlotter(String applicationTitle, String chartTitle, int[] data, ArrayList<Calendar> timeList,
			String SVID) {
		super(applicationTitle);
		// TODO Auto-generated constructor stub

		final JFreeChart chart = ChartFactory.createTimeSeriesChart(chartTitle, "Time of the Day", "Cycle Slips",
				createDatasetCS(timeList, data, SVID), true, true, false);

		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 370));
		chartPanel.setMouseZoomable(true, false);

		setContentPane(chartPanel);

	}

	public GraphPlotter(String applicationTitle, String chartTitle, ArrayList<Double> data, String SVID) {
		super(applicationTitle);
		// TODO Auto-generated constructor stub

		final JFreeChart chart = ChartFactory.createXYLineChart(chartTitle, "X-axis", "Cycle Slip",
				createDatasetCS(data, SVID));
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 370));
		chartPanel.setMouseZoomable(true, false);

		setContentPane(chartPanel);

	}

	public GraphPlotter(String applicationTitle, String chartTitle, ArrayList<Double> prData, ArrayList<Double> cpData,
			String SVID) {
		super(applicationTitle);
		// TODO Auto-generated constructor stub

		final JFreeChart chart = ChartFactory.createXYLineChart(chartTitle, "X-axis", "PR-CP compare",
				createDatasetPRCP(prData, cpData, SVID));
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 370));
		chartPanel.setMouseZoomable(true, false);

		setContentPane(chartPanel);

	}

	public GraphPlotter(String applicationTitle, String chartTitle, ArrayList<Calendar> timeList,
			HashMap<String, ArrayList<Double>> ErrMap, long max, String path) {
		super(applicationTitle);
		// TODO Auto-generated constructor stub

		final JFreeChart chart = ChartFactory.createTimeSeriesChart(chartTitle, "Time of the Day", "Error",
				createDatasetError(timeList, ErrMap), true, true, false);

		XYPlot plot = chart.getXYPlot();
		NumberAxis axis = (NumberAxis) plot.getRangeAxis();
		axis.setAutoRange(false);
		axis.setRange(0, max);
		try {
			ChartUtils.saveChartAsPNG(new File(path), chart, 600, 400);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 370));
		chartPanel.setMouseZoomable(true, false);

		setContentPane(chartPanel);

	}

	public GraphPlotter(String applicationTitle, String chartTitle, ArrayList<Calendar> timeList,
			HashMap<String, ArrayList<Double>> ErrMap) {
		super(applicationTitle);
		// TODO Auto-generated constructor stub

		final JFreeChart chart = ChartFactory.createTimeSeriesChart(chartTitle, "Time of the Day", "Error",
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

	private TimePeriodValuesCollection createDatasetCS(ArrayList<Calendar> timeList, int[] data, String SVID) {
		TimePeriodValuesCollection coll = new TimePeriodValuesCollection();

		TimePeriodValues series = new TimePeriodValues(SVID);
		ArrayList<Integer> list = (ArrayList<Integer>) Arrays.stream(data).boxed().collect(Collectors.toList());
		for (int i = 0; i < list.size(); i++) {

			series.add(new Second(timeList.get(i).getTime()), list.get(i));
		}

		coll.addSeries(series);

		return coll;

	}

	private XYDataset createDatasetCS(ArrayList<Double> data, String SVID) {
		final XYSeries cs = new XYSeries(SVID);
		for (int i = 0; i < data.size(); i++) {
			cs.add(i, data.get(i));
		}
		final XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(cs);
		return dataset;

	}

	private XYDataset createDatasetPRCP(ArrayList<Double> prData, ArrayList<Double> cpData, String SVID) {
		final XYSeries pr = new XYSeries("PR");
		final XYSeries cp = new XYSeries("CP");
		for (int i = 0; i < prData.size(); i++) {
			pr.add(i, prData.get(i));
			cp.add(i, cpData.get(i));
		}
		final XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(pr);
		dataset.addSeries(cp);
		return dataset;

	}

	private TimePeriodValuesCollection createDatasetError(ArrayList<Calendar> timeList,
			HashMap<String, ArrayList<Double>> _map) {
		TimePeriodValuesCollection coll = new TimePeriodValuesCollection();

		for (String _type : _map.keySet()) {
			TimePeriodValues series = new TimePeriodValues(_type);
			ArrayList<Double> _list = _map.get(_type);
			for (int i = 0; i < _list.size(); i++) {

				series.add(new Second(timeList.get(i).getTime(), TimeZone.getTimeZone("UTC"), Locale.UK), _list.get(i));

			}

			coll.addSeries(series);
		}
		return coll;
	}
}
