package com.RINEX_parser.ComputeUserPos.Regression.RobustRegression;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares;

import com.RINEX_parser.models.Satellite;

public class RobustFit {

	public static ArrayList<Satellite> process(ArrayList<Satellite> SV) {

		List<Point> points = SV.stream().map(i -> new Point(i)).collect(Collectors.toList());
		ModelManager<StateModel> manager = new StateModelManager();
		LeastMedianOfSquares<StateModel, Point> lms = new LeastMedianOfSquares<>(234234, 100, 1e13, 0.99, manager,
				Point.class);

		lms.setModel(StateGenerator::new, Residual::new);
		if (!lms.process(points)) {
			System.err.println("Robust fit failed!");
			throw new RuntimeException("Robust fit failed!");
		}

		List<Point> inliers = lms.getMatchSet();
		ArrayList<Satellite> inlierSV = inliers.stream().map(i -> i.getSat())
				.collect(Collectors.toCollection(ArrayList::new));

		return inlierSV;

	}

}
