package com.RINEX_parser.ComputeUserPos.Regression.RobustRegression;

import java.util.List;

import org.ddogleg.fitting.modelset.DistanceFromModel;

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

		double[] h = pt.getH();
		double y = pt.getY();
		double y_hat = (h[0] * xyz[0]) + (h[1] * xyz[1]) + (h[2] * xyz[2]) + (h[3] * b);
		return Math.pow(y - y_hat, 2);

	}

	@Override
	public void distances(List<Point> points, double[] distance) {
		// TODO Auto-generated method stub
		for (int i = 0; i < points.size(); i++) {
			Point pt = points.get(i);
			distance[i] = distance(pt);
		}
		System.out.print("");
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

}
