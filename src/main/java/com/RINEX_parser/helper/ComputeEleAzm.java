package com.RINEX_parser.helper;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import java.util.Arrays;

import org.ejml.simple.SimpleMatrix;

import com.RINEX_parser.utility.ECEFtoLatLon;

public class ComputeEleAzm {
	public static double[] computeEleAzm(double[] _userECEF, double[] _satECEF) {

		SimpleMatrix userECEF = new SimpleMatrix(new double[][] { Arrays.copyOfRange(_userECEF, 0, 3) }).transpose();
		SimpleMatrix satECEF = new SimpleMatrix(new double[][] { _satECEF }).transpose();
		SimpleMatrix range = satECEF.minus(userECEF);
		double[] userLLA = ECEFtoLatLon.ecef2lla(_userECEF);
		// User lat and lon
		double lat = userLLA[0] * (Math.PI / 180);
		double lon = userLLA[1] * (Math.PI / 180);
		// Reference https://www.ngs.noaa.gov/CORS/Articles/SolerEisemannJSE.pdf
		SimpleMatrix RotationMatrix = new SimpleMatrix(
				new double[][] { { -sin(lon), cos(lon), 0 }, { -sin(lat) * cos(lon), -sin(lat) * sin(lon), cos(lat) },
						{ cos(lat) * cos(lon), cos(lat) * sin(lon), sin(lat) } });
		SimpleMatrix ENU = RotationMatrix.mult(range);
		double E = ENU.get(0, 0);
		double N = ENU.get(1, 0);
		double U = ENU.get(2, 0);
		double ElevAngle = Math.abs(Math.atan(U / Math.sqrt(Math.pow(E, 2) + Math.pow(N, 2))));
		if (U < 0) {
			ElevAngle *= -1;
		}
		double AzmAngle = Math.abs(Math.atan(E / N));
		AzmAngle += E > 0 ? (N < 0 ? Math.PI - (2 * AzmAngle) : 0) : (N < 0 ? -Math.PI : -(2 * AzmAngle));

		return new double[] { ElevAngle, AzmAngle };

	}

}