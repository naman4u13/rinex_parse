package com.RINEX_parser.ComputeUserPos.Regression.RobustRegression;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.ddogleg.fitting.modelset.ModelGenerator;

import com.RINEX_parser.ComputeUserPos.Regression.WLS;
import com.RINEX_parser.models.Satellite;

public class StateGenerator implements ModelGenerator<StateModel, Point> {
	private final static double SpeedofLight = 299792458;

	@Override
	public boolean generate(List<Point> dataSet, StateModel output) {
		ArrayList<Satellite> SV = dataSet.stream().map(i -> i.getSat())
				.collect(Collectors.toCollection(ArrayList::new));

		double[] PR = SV.stream().mapToDouble(i -> i.getPseudorange()).toArray();
		WLS wls = new WLS(SV, new double[] { 0, 0, 0 }, null, null, null, null);
		try {
			output.setXYZ(wls.getEstECEF(PR));
		} catch (Exception e) {
			// TODO: handle exception
			return false;
		}
		output.setB(SpeedofLight * wls.getRcvrClkOff());
		return true;
	}

	@Override
	public int getMinimumPoints() {
		// TODO Auto-generated method stub
		return 4;
	}

}
