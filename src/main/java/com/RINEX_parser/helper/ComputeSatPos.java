package com.RINEX_parser.helper;

import java.util.Arrays;
import java.util.TimeZone;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;

import com.RINEX_parser.models.NavigationMsg;

public class ComputeSatPos {
	public static Object[] computeSatPos(NavigationMsg Sat, double tSV) {
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

		double ascNode = Sat.getOMEGA0() + ((Sat.getOMEGA_DOT() - OMEGA_E_DOT) * tk) - (OMEGA_E_DOT * Sat.getTOE());// Corrected
																													// longitude
																													// of
																													// ascending
																													// node

		double xk_ECEF = (xk_orbital * Math.cos(ascNode)) - (yk_orbital * Math.cos(ik) * Math.sin(ascNode));
		double yk_ECEF = (xk_orbital * Math.sin(ascNode)) + (yk_orbital * Math.cos(ik) * Math.cos(ascNode));
		double zk_ECEF = yk_orbital * Math.sin(ik);

		// Deriving SV_clock_drift
		// Source - The study of GPS Time Transfer based on extended Kalman filter -
		// https://ieeexplore.ieee.org/document/6702097
		double Ek_dot = n / (1 - (Sat.getE() * Math.cos(Ek)));
		double relativistic_error_dot = F * Sat.getE() * Sat.getSqrt_A() * Math.cos(Ek) * Ek_dot;
		double SV_clock_drift_derived = Sat.getSV_clock_drift() + (2 * Sat.getSV_clock_drift_rate() * (t - TOC))
				+ relativistic_error_dot;

		// Deriving Satellite Velocity Vector
		// Source -
		// https://www.researchgate.net/publication/228995031_GPS_Satellite_Velocity_and_Acceleration_Determination_using_the_Broadcast_Ephemeris

		// RotMatrix is defined as the rotation matrix from the ICDorb to the ECEF.
		// orbital coordinate system used in the ICD-GPS-200c (ICDorb) is different from
		// the ‘‘natural ’’orbital system.

		double[][] _RotMartix = {
				{ Math.cos(ascNode), -Math.sin(ascNode) * Math.cos(ik), Math.sin(ascNode) * Math.sin(ik) },
				{ Math.sin(ascNode), Math.cos(ascNode) * Math.cos(ik), -Math.cos(ascNode) * Math.sin(ik) },
				{ 0, Math.sin(ik), Math.cos(ik) } };
		double ascNode_dot = Sat.getOMEGA_DOT() - OMEGA_E_DOT;
		double[][] _RotMatrixDot = {
				{ -Math.sin(ascNode) * ascNode_dot, -Math.cos(ascNode) * Math.cos(ik) * ascNode_dot,
						Math.cos(ascNode) * Math.sin(ik) * ascNode_dot },
				{ Math.cos(ascNode) * ascNode_dot, -Math.sin(ascNode) * Math.cos(ik) * ascNode_dot,
						Math.sin(ascNode) * Math.sin(ik) * ascNode_dot },
				{ 0, 0, 0 } };
		double[][] _rICDorb = { { rk * Math.cos(uk) }, { rk * Math.sin(uk) }, { 0 } };
		double Vk_dot = (A * A * Math.sqrt(1 - (Sat.getE() * Sat.getE())) * n) / (rk * rk);
		double radius_corr_dot = -2 * ((Sat.getCrc() * Math.sin(2 * argument_of_latitude))
				- (Sat.getCrs() * Math.cos(2 * argument_of_latitude))) * Vk_dot;
		double rk_dot = (A * Sat.getE() * Math.sin(Ek) * Ek_dot) + radius_corr_dot;
		double argument_of_latitude_correction_dot = -2 * ((Sat.getCuc() * Math.sin(2 * argument_of_latitude))
				- (Sat.getCus() * Math.cos(2 * argument_of_latitude))) * Vk_dot;
		double uk_dot = Vk_dot + argument_of_latitude_correction_dot;
		double[][] _rICDorbDot = { { (rk_dot * Math.cos(uk)) - (rk * Math.sin(uk) * uk_dot) },
				{ (rk_dot * Math.sin(uk)) + (rk * Math.cos(uk) * uk_dot) }, { 0 } };

		SimpleMatrix RotMatrix = new SimpleMatrix(_RotMartix);
		SimpleMatrix RotMatrixDot = new SimpleMatrix(_RotMatrixDot);
		SimpleMatrix rICDorb = new SimpleMatrix(_rICDorb);
		SimpleMatrix rICDorbDot = new SimpleMatrix(_rICDorbDot);

		SimpleMatrix _SV_velocity = (RotMatrixDot.mult(rICDorb)).plus(RotMatrix.mult(rICDorbDot));
		double[] SV_velocity = IntStream.range(0, 3).mapToDouble(i -> _SV_velocity.get(i, 0)).toArray();
		double modVel = Arrays.stream(SV_velocity).map(i -> i * i).reduce(0.0, (i, j) -> i + j);
		modVel = Math.sqrt(modVel);
		double[] ECEF_SatClkOff = new double[] { xk_ECEF, yk_ECEF, zk_ECEF, SatClockOffset };

//		double Vk_dot2 = (Ek_dot * Math.sqrt(1 - (Sat.getE() * Sat.getE()))) / (1 - (Sat.getE() * Math.cos(Ek)));
//		double Vk_dot3 = Math.sin(Ek) * Ek_dot * (1.0 + (Sat.getE() * Math.cos(Vk)))
//				/ (Math.sin(Vk) * (1.0 - (Sat.getE() * Math.cos(Ek))));
//		double ik_dot2 = Sat.getIDOT() + (2 * Vk_dot2 * ((Sat.getCis() * Math.cos(2 * argument_of_latitude))
//				- (Sat.getCic() * Math.sin(2 * argument_of_latitude))));
//		double uk_dot2 = Vk_dot2 - (2 * ((Sat.getCuc() * Math.sin(2 * argument_of_latitude))
//				- (Sat.getCus() * Math.cos(2 * argument_of_latitude))) * Vk_dot2);
//		double rk_dot2 = (A * Sat.getE() * Math.sin(Ek) * Ek_dot)
//				+ (-2 * ((Sat.getCrc() * Math.sin(2 * argument_of_latitude))
//						- (Sat.getCrs() * Math.cos(2 * argument_of_latitude))) * Vk_dot2);
//		double ascNode_dot2 = Sat.getOMEGA_DOT() - OMEGA_E_DOT;
//		double vxplane = (rk_dot2 * Math.cos(uk)) - (rk * Math.sin(uk) * uk_dot2);
//		double vyplane = (rk_dot2 * Math.sin(uk)) + (rk * Math.cos(uk) * uk_dot2);
//		double vx2 = (-xk_orbital * ascNode_dot2 * Math.sin(ascNode)) + (vxplane * Math.cos(ascNode))
//				- (vyplane * Math.sin(ascNode) * Math.cos(ik))
//				- (yk_orbital * ((ascNode_dot2 * Math.cos(ascNode) * Math.cos(ik))
//						- (ik_dot2 * Math.sin(ascNode) * Math.sin(ik))));
//		double vy2 = (xk_orbital * ascNode_dot2 * Math.cos(ascNode)) + (vxplane * Math.sin(ascNode))
//				+ (vyplane * Math.cos(ascNode) * Math.cos(ik))
//				- (yk_orbital * ((ascNode_dot2 * Math.sin(ascNode) * Math.cos(ik))
//						+ (ik_dot2 * Math.cos(ascNode) * Math.sin(ik))));
//		double vz2 = (yk_orbital * ik_dot2 * Math.cos(ik)) + (vyplane * Math.sin(ik));
//		double[] SV_velocity2 = { vx2, vy2, vz2 };
		return new Object[] { ECEF_SatClkOff, SV_velocity, SV_clock_drift_derived };

	}

}
