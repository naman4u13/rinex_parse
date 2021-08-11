package com.RINEX_parser.utility;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

public class Matrix {

	public static SimpleMatrix getPseudoInverse(SimpleMatrix A) {
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
