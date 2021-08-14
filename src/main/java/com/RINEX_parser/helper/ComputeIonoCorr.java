package com.RINEX_parser.helper;

import java.util.Calendar;

import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.ionosphere.KlobucharIonoModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import com.RINEX_parser.constants.Constellation;
import com.RINEX_parser.models.IonoCoeff;

public class ComputeIonoCorr {
	final static double Earth_Radius = 6378136.3;// in meter
	final static double max_electron_density = 350000;// in meter
	final static double SpeedofLight = 299792458;

	public static double computeIonoCorr(double ElevAng_rad, double AzmAng_rad, double userLat_deg, double userLong_deg,
			double tRX, IonoCoeff ionoCoeff, double freq, Calendar time) {

		tRX = tRX % 86400;
		double userLat_rad = Math.toRadians(userLat_deg);
		double userLong_rad = Math.toRadians(userLong_deg);

		double earth_central_angle = (0.0137 / ((ElevAng_rad / Math.PI) + 0.11)) - 0.022;
		double IPP_lat = (userLat_rad / Math.PI) + (earth_central_angle * Math.cos(AzmAng_rad));
		if (IPP_lat > 0.416) {
			IPP_lat = 0.416;
		} else if (IPP_lat < -0.416) {
			IPP_lat = -0.416;
		}
		double IPP_long = (userLong_rad / Math.PI)
				+ ((earth_central_angle * Math.sin(AzmAng_rad)) / Math.cos(IPP_lat * Math.PI));

		double geomagnetic_lat = IPP_lat + (0.064 * Math.cos((IPP_long - 1.617) * Math.PI));

		double local_time = 4.32 * (1E4) * (IPP_long) + tRX;
		if (local_time >= 86400) {
			local_time = local_time - 86400;
		} else if (local_time < 0) {
			local_time = local_time + 86400;
		}

		double obliquity_factor = 1 + (16 * (Math.pow(0.53 - (ElevAng_rad / Math.PI), 3)));

		double PER = ionoCoeff.getBeta0() + (ionoCoeff.getBeta1() * geomagnetic_lat)
				+ (ionoCoeff.getBeta2() * Math.pow(geomagnetic_lat, 2))
				+ (ionoCoeff.getBeta3() * Math.pow(geomagnetic_lat, 3));
		if (PER < 72000) {
			PER = 72000;
		}

		double phase = (2 * Math.PI * (local_time - 50400)) / PER;

		double AMP = ionoCoeff.getAlpha0() + (ionoCoeff.getAlpha1() * geomagnetic_lat)
				+ (ionoCoeff.getAlpha2() * Math.pow(geomagnetic_lat, 2))
				+ (ionoCoeff.getAlpha3() * Math.pow(geomagnetic_lat, 3));
		if (AMP < 0) {
			AMP = 0;
		}

		double iono_time;
		if (Math.abs(phase) < 1.57) {
			iono_time = obliquity_factor * (5E-9 + (AMP * (1 - (Math.pow(phase, 2) / 2) + (Math.pow(phase, 4) / 24))));

		} else {
			iono_time = obliquity_factor * 5E-9;

		}
		double iono_corr = iono_time * SpeedofLight;

		double freqRatio = Math.pow(Constellation.frequency.get('G').get(1), 2) / Math.pow(freq, 2);

		double[] alpha = new double[] { ionoCoeff.getAlpha0(), ionoCoeff.getAlpha1(), ionoCoeff.getAlpha2(),
				ionoCoeff.getAlpha3() };
		double[] beta = new double[] { ionoCoeff.getBeta0(), ionoCoeff.getBeta1(), ionoCoeff.getBeta2(),
				ionoCoeff.getBeta3() };
		KlobucharIonoModel kim = new KlobucharIonoModel(alpha, beta);
		AbsoluteDate date = new AbsoluteDate(time.getTime(), TimeScalesFactory.getGPS());
		GeodeticPoint pt = new GeodeticPoint(userLat_rad, userLong_rad, 0);
		// Orekit API derivded Klobuchlar delay, used to validate estimated delay
		double _delay = kim.pathDelay(date, pt, ElevAng_rad, AzmAng_rad, freq, null);
		double delay = iono_corr * freqRatio;

		if (Math.abs(_delay - delay) > 0.1) {
			System.err.println("Klob Iono Error est wrong ->  " + (_delay - delay) + " Elevation -> "
					+ Math.toDegrees(ElevAng_rad));
		}

		return delay;

	}

}
