package com.RINEX_parser.helper;

import java.util.Calendar;
import java.util.stream.IntStream;

import org.orekit.models.earth.Geoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class ComputeTropoCorr {

	/*
	 * Col Header - Barometric Pressure(P), Temperature(T), Relative Humidity(Rh),
	 * Temperature Lapse Rate (b) and Water Vapour Pressure Height Factor (l).
	 */
	/*
	 * Row Header(Latitude) - 15 deg or less, 30 deg, 45 deg, 60 deg, 75 deg or
	 * greater
	 */

	// Average Meteorological Parameters for Tropospheric Delay
	private final double[][] Y0;

	// Seasonal Meteorological Parameters for Tropospheric Delay
	private final double[][] Ydelta;

	private final double[][] CoeffAvgDry;
	private final double[][] CoeffAmpDry;

	private final double[] heightCorr;
	private final double[][] CoeffAvgWet;
	// site Lat, rcvr ellipsoidal Height
	private double lat, H;
	// Day
	private int D;
	private double[] ZD;
	private double[] y;
	private double[] coeffDry;
	private double[] coeffWet;

	public ComputeTropoCorr(double[] llh, Calendar time, Geoid geoid) {
		// TODO Auto-generated constructor stub
		Y0 = new double[][] { { 1013.25, 299.65, 75.0, 6.30e-3, 2.77 }, { 1017.25, 294.15, 80.0, 6.05e-3, 3.15 },
				{ 1015.75, 283.15, 76.0, 5.58e-3, 2.57 }, { 1011.75, 272.15, 77.5, 5.39e-3, 1.81 },
				{ 1013.00, 263.65, 82.5, 4.53e-3, 1.55 } };
		Ydelta = new double[][] { { 0.00, 0.00, 0.0, 0.00, 0.00 }, { -3.75, 7.00, 0.0, 0.25e-3, 0.33 },
				{ -2.25, 11.00, -1.0, 0.32e-3, 0.46 }, { -1.75, 15.00, -2.5, 0.81e-3, 0.74 },
				{ -0.50, 14.50, 2.5, 0.62e-3, 0.30 } };
		CoeffAvgDry = new double[][] { { 1.2769934 * 1e-3, 2.9153695 * 1e-3, 62.610505 * 1e-3 },
				{ 1.2683230 * 1e-3, 2.9152299 * 1e-3, 62.837393 * 1e-3 },
				{ 1.2465397 * 1e-3, 2.9288445 * 1e-3, 63.721774 * 1e-3 },
				{ 1.2196049 * 1e-3, 2.9022565 * 1e-3, 63.824265 * 1e-3 },
				{ 1.2045996 * 1e-3, 2.9024912 * 1e-3, 62.258455 * 1e-3 } };
		CoeffAmpDry = new double[][] { { 0.0, 0.0, 0.0 }, { 1.2709626 * 1e-5, 2.1414979 * 1e-5, 9.0128400 * 1e-5 },
				{ 2.6523662 * 1e-5, 3.0160779 * 1e-5, 4.3497037 * 1e-5 },
				{ 3.4000452 * 1e-5, 7.2562722 * 1e-5, 84.795348 * 1e-5 },
				{ 4.1202191 * 1e-5, 11.723375 * 1e-5, 170.37206 * 1e-5 } };
		heightCorr = new double[] { 2.53 * 1e-5, 5.49 * 1e-3, 1.14 * 1e-3 };
		CoeffAvgWet = new double[][] { { 5.8021897 * 1e-4, 1.4275268 * 1e-3, 4.3472961 * 1e-2 },
				{ 5.6794847 * 1e-4, 1.5138625 * 1e-3, 4.6729510 * 1e-2 },
				{ 5.8118019 * 1e-4, 1.4572752 * 1e-3, 4.3908931 * 1e-2 },
				{ 5.9727542 * 1e-4, 1.5007428 * 1e-3, 4.4626982 * 1e-2 },
				{ 6.1641693 * 1e-4, 1.7599082 * 1e-3, 5.4736038 * 1e-2 } };
		this.lat = llh[0];
		this.D = time.get(Calendar.DAY_OF_YEAR);

		AbsoluteDate date = new AbsoluteDate(time.getTime(), TimeScalesFactory.getGPS());
		double lon = llh[1];
		// Geoid Height
		double geoidH = geoid.getUndulation(lat, lon, date);
		// Ellipsoid Height
		double ellipH = llh[2];
		// Orthometric Height or MSL
		H = ellipH - geoidH;
		y = new double[5];
		coeffDry = new double[3];
		coeffWet = new double[3];
		initiate();

	}

	// Based on UNB3m tropo delay model
	private void initiate() {

		int Dmin;

		if (lat > 0) {
			// Northern Latitude
			Dmin = 28;
		} else {
			// Southern Latitude
			Dmin = 211;
		}
		lat = Math.abs(lat);

		double row = lat / 15;
		int row1 = (int) Math.floor(row);
		int row2 = (int) Math.ceil(row);
		if (row1 == 0) {

			y = timeCorr(D, Dmin, Y0[0], Ydelta[0]);
			coeffDry = timeCorr(D, Dmin, CoeffAvgDry[0], CoeffAmpDry[0]);
			coeffWet = CoeffAvgWet[0];
		} else if (row1 == 5 || row2 == 6) {
			y = timeCorr(D, Dmin, Y0[4], Ydelta[4]);
			coeffDry = timeCorr(D, Dmin, CoeffAvgDry[4], CoeffAmpDry[4]);
			coeffWet = CoeffAvgWet[4];
		} else if (row1 == row2) {
			y = timeCorr(D, Dmin, Y0[row1 - 1], Ydelta[row1 - 1]);
			coeffDry = timeCorr(D, Dmin, CoeffAvgDry[row1 - 1], CoeffAmpDry[row1 - 1]);
			coeffWet = CoeffAvgWet[row1 - 1];
		} else {

			double x1 = 15 * row1;
			double x2 = 15 * row2;
			double[] y0 = interpolate(Y0[row1 - 1], Y0[row2 - 1], x1, x2, lat);
			double[] ydelta = interpolate(Ydelta[row1 - 1], Ydelta[row2 - 1], x1, x2, lat);
			y = timeCorr(D, Dmin, y0, ydelta);
			double[] coeffAvgDry = interpolate(CoeffAvgDry[row1 - 1], CoeffAvgDry[row2 - 1], x1, x2, lat);
			double[] coeffAmpDry = interpolate(CoeffAmpDry[row1 - 1], CoeffAmpDry[row2 - 1], x1, x2, lat);
			coeffDry = timeCorr(D, Dmin, coeffAvgDry, coeffAmpDry);
			coeffWet = interpolate(CoeffAvgWet[row1 - 1], CoeffAvgWet[row2 - 1], x1, x2, lat);

		}

		// Compute Slant Delay
		// Zenith Delay
		computeZenithDelay(y);
		System.out.println("ZENITH DELAY =  " + ZD[0] + "  " + ZD[1]);
	}

	public double getSlantDelay(double E) {
		// Map
		double[] map = computeMappingFun(coeffDry, coeffWet, H, E);
		double SD = (ZD[0] * map[0]) + (ZD[1] * map[1]);

		return SD;
	}

	// Based on Saastamonien Delay model
	private void computeZenithDelay(double[] y) {

		// Barometric Pressure(P)
		double P = y[0];
		// Temperature(T)
		double T = y[1];
		// Relative Humidity(Rh)
		double Rh = y[2];
		// Temperature Lapse Rate (b)
		double b = y[3];
		// Water Vapour Pressure Height Factor (l).
		double l = y[4];

		// Saturation Vapour Pressure
		double es = 0.01 * Math.exp((1.2378847 * Math.pow(10, -5) * Math.pow(T, 2)) - (1.9121316 * Math.pow(10, -2) * T)
				+ 33.93711047 - (6.3431645 * Math.pow(10, 3) / T));
		// Enhancement factor
		double fw = 1.00062 + (3.14 * (1E-6) * P) + (5.6 * (1E-7) * Math.pow(T - 273.15, 2));

		// Water Vapor Pressure

		double e = (Rh / 100) * es * fw;

		double k1 = 77.60;
		double _k2 = 16.6;
		double k3 = 377600;
		double R = 287.054;
		double g = 9.80665;
		double gm = 9.784 * (1 - (2.66 * (1E-3) * Math.cos(2 * lat * Math.PI / 180)) - (2.8 * (1e-7) * H));
		double _l = l + 1;
		double Tm = T * (1 - (b * R / (gm * _l)));

		double zd_dry = ((1E-6) * k1 * R / gm) * P * Math.pow(1 - (b * H / T), g / (R * b));
		double zd_wet = (((1E-6) * ((Tm * _k2) + k3) * R * e) / (T * ((gm * _l) - (b * R))))
				* Math.pow(1 - (b * H / T), (_l * g / (R * b)) - 1);
		ZD = new double[] { zd_dry, zd_wet };
	}

	// Based on Niell mapping function
	private double[] computeMappingFun(double[] coeffDry, double[] coeffWet, double H, double E) {

		double m_dry = normMariniMap(E, coeffDry);
		double delta_m = ((1 / Math.sin(E)) - normMariniMap(E, heightCorr)) * (H / 1000);
		double M_dry = m_dry + delta_m;
		double M_wet = normMariniMap(E, coeffWet);
		return new double[] { M_dry, M_wet };

	}

	// Marini mapping normalised to unity at zenith:
	private double normMariniMap(double E, double[] coeff) {
		double a = coeff[0];
		double b = coeff[1];
		double c = coeff[2];
		double S = Math.sin(E);
		double m = 1 + (a / (1 + b / (1 + c))) / (S + (a / (S + b / (S + c))));
		return m;
	}

	private double[] interpolate(double[] y1, double[] y2, double x1, double x2, double x) {
		int n = y1.length;
		double[] interY = IntStream.range(0, n).mapToDouble(i -> y1[i] + (y2[i] - y1[i]) * ((x - x1) / (x2 - x1)))
				.toArray();
		return interY;
	}

	private double[] timeCorr(int D, int Dmin, double[] y0, double[] ydelta) {
		int n = y0.length;
		double[] y = IntStream.range(0, n)
				.mapToDouble(i -> y0[i] - (ydelta[i] * (Math.cos(2 * Math.PI * (D - Dmin) / 365.25)))).toArray();
		return y;
	}

}
