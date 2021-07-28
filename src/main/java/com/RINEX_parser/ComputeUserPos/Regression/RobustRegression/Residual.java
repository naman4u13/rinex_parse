package com.RINEX_parser.ComputeUserPos.Regression.RobustRegression;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

import com.RINEX_parser.models.Satellite;
import com.RINEX_parser.utility.ECEFtoLatLon;

public class Residual implements DistanceFromModel<StateModel, Point> {

	private double[] xyz;
	private double b;

	@Override
	public void setModel(StateModel model) {
		// TODO Auto-generated method stub
		xyz = model.getXYZ();
		b = model.getB();
	}

	@Override
	public double distance(Point pt) {
		// TODO Auto-generated method stub
		Satellite sat = pt.getSat();

		double z_hat = Math.sqrt(IntStream.range(0, 3).mapToDouble(i -> sat.getECI()[i] - xyz[i]).map(i -> i * i).sum())
				+ b;
		double z = sat.getPseudorange();

		return Math.pow(z - z_hat, 2);
	}

	@Override
	public void distances(List<Point> points, double[] distance) {
		// TODO Auto-generated method stub
		int n = points.size();
		distance = new double[distance.length];
		double[][] h = new double[n][3];
		ArrayList<Satellite> SV = points.stream().map(i -> i.getSat()).collect(Collectors.toCollection(ArrayList::new));
		double[] res = new double[n];
		for (int i = 0; i < n; i++) {
			Satellite sat = SV.get(i);
			double z_hat = Math
					.sqrt(IntStream.range(0, 3).mapToDouble(j -> sat.getECI()[j] - xyz[j]).map(j -> j * j).sum());
			final double _z_hat = z_hat;

			h[i] = IntStream.range(0, 3).mapToDouble(j -> -(sat.getECI()[j] - xyz[j]) / _z_hat).toArray();

			double z = sat.getPseudorange();
			z_hat += b;
			res[i] = z - z_hat;
		}
		SimpleMatrix e_hat = new SimpleMatrix(n, 1, false, res);
		double[] llh = ECEFtoLatLon.ecef2lla(xyz);
		double lat = Math.toRadians(llh[0]);
		double lon = Math.toRadians(llh[1]);
		double[][] f = new double[][] { { -Math.sin(lon), Math.cos(lon), 0 },
				{ -Math.sin(lat) * Math.cos(lon), -Math.sin(lat) * Math.sin(lon), Math.cos(lat) },
				{ Math.cos(lat) * Math.cos(lon), Math.cos(lat) * Math.sin(lon), Math.sin(lat) } };
		SimpleMatrix H = new SimpleMatrix(h);
		SimpleMatrix F = new SimpleMatrix(f);
		SimpleMatrix psInvH = getPseudoInv(H);
		SimpleMatrix S = H.mult(psInvH).minus(SimpleMatrix.identity(n));
		SimpleMatrix psInvS = getPseudoInv(S);

		SimpleMatrix M = F.mult(psInvH);
		SimpleMatrix e = psInvS.mult(e_hat);

		for (int i = 0; i < n; i++) {
			distance[i] = (Math.pow(M.get(0, i), 2) + Math.pow(M.get(1, i), 2)) * Math.pow(e.get(i), 2);
		}

	}

	@Override
	public Class<Point> getPointType() {
		// TODO Auto-generated method stub
		return Point.class;
	}

	@Override
	public Class<StateModel> getModelType() {
		// TODO Auto-generated method stub
		return StateModel.class;
	}

	private SimpleMatrix getPseudoInv(SimpleMatrix A) {
		SimpleSVD<SimpleMatrix> svd = new SimpleSVD<SimpleMatrix>(A.getMatrix(), true);
		int rank = svd.rank();
		double[] singularVal = svd.getSingularValues();
		int n = singularVal.length;
		double eps = Math.ulp(1.0);
		double[][] _Wplus = new double[n][n];
		double maxW = Double.MIN_VALUE;
		for (int i = 0; i < n; i++) {
			maxW = Math.max(singularVal[i], maxW);
		}
		double tolerance = Math.max(A.numRows(), A.numCols()) * eps * maxW;
		for (int i = 0; i < n; i++) {
			double val = singularVal[i];
			if (val < tolerance) {
				continue;
			}
			_Wplus[i][i] = 1 / val;
		}
		SimpleMatrix Wplus = new SimpleMatrix(_Wplus);
		Wplus = Wplus.transpose();
		SimpleMatrix U = svd.getU();
		SimpleMatrix V = svd.getV();
		SimpleMatrix Ut = U.transpose();
		SimpleMatrix pseudoInv = V.mult(Wplus).mult(Ut);
		return pseudoInv;
	}

}
