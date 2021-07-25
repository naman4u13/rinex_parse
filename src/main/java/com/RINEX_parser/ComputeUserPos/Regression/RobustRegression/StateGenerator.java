package com.RINEX_parser.ComputeUserPos.Regression.RobustRegression;

import java.util.List;

import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

public class StateGenerator implements ModelGenerator<StateModel, Point> {

	@Override
	public boolean generate(List<Point> dataSet, StateModel output) {

		int len = dataSet.size();
		double[][] _h = new double[len][];
		double[] _y = new double[len];
		for (int i = 0; i < len; i++) {
			Point pt = dataSet.get(i);
			_h[i] = pt.getH();
			_y[i] = pt.getY();
		}

		SimpleMatrix h = new SimpleMatrix(_h);
		SimpleMatrix y = new SimpleMatrix(len, 1, false, _y);

		SimpleSVD<SimpleMatrix> svd = new SimpleSVD<SimpleMatrix>(h.getMatrix(), true);

		double[] singularVal = svd.getSingularValues();
		int n = singularVal.length;
		double eps = Math.ulp(1.0);
		double[][] _Wplus = new double[n][n];
		double maxW = Double.MIN_VALUE;
		for (int i = 0; i < n; i++) {
			maxW = Math.max(singularVal[i], maxW);
		}
		double tolerance = Math.max(h.numRows(), h.numCols()) * eps * maxW;
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
		SimpleMatrix x = V.mult(Wplus).mult(Ut).mult(y);
		output.setXYZ(new double[] { x.get(0), x.get(1), x.get(2) });
		output.setB(x.get(3));
		return true;
	}

	@Override
	public int getMinimumPoints() {
		// TODO Auto-generated method stub
		return 4;
	}

}
