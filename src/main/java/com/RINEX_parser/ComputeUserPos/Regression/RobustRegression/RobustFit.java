package com.RINEX_parser.ComputeUserPos.Regression.RobustRegression;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares;

import com.RINEX_parser.models.Satellite;

public class RobustFit {
	private final static double SpeedofLight = 299792458;

	public static ArrayList<Satellite> process(ArrayList<Satellite> SV, double[] rxECEF, double rxClkOff) {

		List<Point> points = setPoints(SV, rxECEF, rxClkOff);
		ModelManager<StateModel> manager = new StateModelManager();
		LeastMedianOfSquares<StateModel, Point> lms = new LeastMedianOfSquares<>(234234, 500, 100, 0.05, manager,
				Point.class);

		lms.setModel(StateGenerator::new, Residual::new);
		if (!lms.process(points)) {
			System.err.println("Robust fit failed!");
			throw new RuntimeException("Robust fit failed!");
		}

		List<Point> inliers = lms.getMatchSet();
		ArrayList<Satellite> inlierSV = inliers.stream().map(i -> SV.get(i.getIndex()))
				.collect(Collectors.toCollection(ArrayList::new));

		return inlierSV;

	}

	private static List<Point> setPoints(ArrayList<Satellite> SV, double[] rxECEF, double rxClkOff) {

		List<Point> points = new ArrayList<Point>();
		int n = SV.size();
		for (int i = 0; i < n; i++) {
			Satellite sat = SV.get(i);
			double pr = sat.getPseudorange();
			double gr_hat = Math.sqrt(IntStream.range(0, 3).mapToDouble(j -> sat.getECI()[j] - rxECEF[j])
					.map(j -> Math.pow(j, 2)).reduce((j, k) -> j + k).getAsDouble());
			double[] h = new double[4];
			IntStream.range(0, 3).forEach(j -> h[j] = -(sat.getECI()[j] - rxECEF[j]) / gr_hat);
			h[3] = 1;
			double pr_hat = gr_hat + (SpeedofLight * rxClkOff);
			double y = pr - pr_hat;
			points.add(new Point(h, y, i));

		}
		return points;

	}

}
