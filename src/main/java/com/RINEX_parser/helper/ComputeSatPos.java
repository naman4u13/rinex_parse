package com.RINEX_parser.helper;

import java.util.TimeZone;

import com.RINEX_parser.models.NavigationMsg;

public class ComputeSatPos {
	public static double[] computeSatPos(NavigationMsg Sat, double tSV) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		double Mu = 3.986005E14; // WGS-84 value of the Earth's universal gravitational parameter
		long NumberSecondsWeek = 604800;
		double OMEGA_E_DOT = 7.2921151467E-5;// WGS-84 value of the Earth's rotation rate
		double F = -4.442807633E-10;

		double A = Math.pow(Sat.getSqrt_A(), 2);
		double n0 = Math.sqrt((Mu / (A * A * A)));

		long TOC = Sat.getTOC();

		/*
		 * double SatClockOffset = Sat.SV_clock_bias + ((tSV - TOC) *
		 * Sat.SV_clock_drift) - Sat.TGD; double new_SatClockOffset = SatClockOffset /
		 * (1 + Sat.SV_clock_drift); double t = tSV - SatClockOffset;
		 *
		 * double tk = t - Sat.TOE; if (tk > 302400) { tk = tk - 604800; } else if (tk <
		 * -302400) { tk = tk + 604800; }
		 */
		double n = n0 + Sat.getDelta_n();
		/*
		 * double Mk = Sat.M0 + (n * tk); // Mean Anomaly
		 *
		 * double delta_Ek = Double.MAX_VALUE; double assumed_Ek = 0; double assumed_Mk
		 * = assumed_Ek - (Sat.e * Math.sin(assumed_Ek)); int count = 0; while (count <
		 * 1000)// && delta_Ek != 0) { delta_Ek = (Mk - assumed_Mk) / (1 - (Sat.e *
		 * Math.cos(assumed_Ek))); assumed_Ek = assumed_Ek + delta_Ek; assumed_Mk =
		 * assumed_Ek - (Sat.e * Math.sin(assumed_Ek)); count++; }
		 *
		 * double Ek = assumed_Ek; // Eccentric anomaly
		 */

		double coeff1 = (Sat.getSV_clock_bias() + ((tSV - TOC) * Sat.getSV_clock_drift()) - Sat.getTGD())
				/ (1 + Sat.getSV_clock_drift());
		double coeff2 = ((n * F * Sat.getSqrt_A()) / (1 + Sat.getSV_clock_drift())) - 1;
		double delta_Ek = 0;
		double assumed_Ek = 0;
		double f_Ek = Sat.getM0() + (n * (tSV - Sat.getTOE() - coeff1));
		double assumed_f_Ek = assumed_Ek + (Sat.getE() * Math.sin(assumed_Ek) * coeff2);
		int count = 0;
		while (count < 1000)// && delta_Ek != 0)
		{
			delta_Ek = (f_Ek - assumed_f_Ek) / (1 + (Sat.getE() * Math.cos(assumed_Ek)) * coeff2);
			assumed_Ek = assumed_Ek + delta_Ek;
			assumed_f_Ek = assumed_Ek + (Sat.getE() * Math.sin(assumed_Ek) * coeff2);
			count++;
		}
		double Ek = assumed_Ek;
		double Mk = Ek - (Sat.getE() * Math.sin(Ek));
		double tk = (Mk - Sat.getM0()) / n;
		double t = tk + Sat.getTOE();
		double relativistic_error = F * Sat.getE() * Sat.getSqrt_A() * Math.sin(Ek);
		double SatClockOffset = Sat.getSV_clock_bias() + ((t - TOC) * Sat.getSV_clock_drift()) - Sat.getTGD()
				+ relativistic_error;

		// System.out.println(Sat.SVID + " " + relativistic_error * 299792458);
		double Vk; // True anomaly

		// Vk = Math.atan((((Math.sqrt(1 - Math.pow(Sat.e, 2))) * Math.sin(Ek)) / (1 -
		// (Sat.e * Math.cos(Ek))))
		// / ((Math.cos(Ek) - Sat.e) / (1 - (Sat.e * Math.cos(Ek)))));

		double num = ((Math.sqrt(1 - Math.pow(Sat.getE(), 2))) * Math.sin(Ek));
		double denom = (Math.cos(Ek) - Sat.getE());

		Vk = Math.atan(num / denom);

		if (denom < 0) {
			if (num > 0) {
				Vk = Math.PI + Vk;
			} else {
				Vk = -Math.PI + Vk;
			}
		}

		double argument_of_latitude = Vk + Sat.getOmega();

		double argument_of_latitude_correction = (Sat.getCus() * Math.sin(2 * argument_of_latitude))
				+ (Sat.getCuc() * Math.cos(2 * argument_of_latitude));
		double radius_correction = (Sat.getCrc() * Math.cos(2 * argument_of_latitude))
				+ (Sat.getCrs() * Math.sin(2 * argument_of_latitude));
		double correction_to_inclination = (Sat.getCic() * Math.cos(2 * argument_of_latitude))
				+ (Sat.getCis() * Math.sin(2 * argument_of_latitude));

		double uk = argument_of_latitude + argument_of_latitude_correction; // corrected_argument_of_latitude
		double rk = (A * (1 - (Sat.getE() * Math.cos(Ek)))) + radius_correction; // corrected_radius
		double ik = Sat.getI0() + correction_to_inclination + (Sat.getIDOT() * tk);// corrected_inclination

		double xk_orbital = rk * Math.cos(uk);
		double yk_orbital = rk * Math.sin(uk);

		double corrected_longitude_of_ascending_node = Sat.getOMEGA0() + ((Sat.getOMEGA_DOT() - OMEGA_E_DOT) * tk)
				- (OMEGA_E_DOT * Sat.getTOE());

		double xk_ECEF = (xk_orbital * Math.cos(corrected_longitude_of_ascending_node))
				- (yk_orbital * Math.cos(ik) * Math.sin(corrected_longitude_of_ascending_node));
		double yk_ECEF = (xk_orbital * Math.sin(corrected_longitude_of_ascending_node))
				+ (yk_orbital * Math.cos(ik) * Math.cos(corrected_longitude_of_ascending_node));
		double zk_ECEF = yk_orbital * Math.sin(ik);

		// double[] LatLon = ECEFtoLatLon.ecef2lla(xk_ECEF, yk_ECEF, zk_ECEF);

		// System.out.println(Sat.SVID + " " + xk_ECEF + " " + yk_ECEF + " " + zk_ECEF +
		// " ");
		// System.out.println("LATLON " + LatLon[0] + " " + LatLon[1] + " " + LatLon[2]
		// + " \n\n ");

		double[] ECEF_SatClkOff = new double[] { xk_ECEF, yk_ECEF, zk_ECEF, SatClockOffset };
		return ECEF_SatClkOff;

	}

}
