package com.RINEX_parser.ComputeUserPos.Regression.RobustRegression;

import java.util.List;
import java.util.stream.IntStream;

import org.ddogleg.fitting.modelset.DistanceFromModel;

import com.RINEX_parser.models.Satellite;

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
		for (int i = 0; i < points.size(); i++) {
			Point pt = points.get(i);
			distance[i] = distance(pt);
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

}
