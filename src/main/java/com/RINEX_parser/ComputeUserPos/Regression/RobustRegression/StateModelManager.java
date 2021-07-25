package com.RINEX_parser.ComputeUserPos.Regression.RobustRegression;

import org.ddogleg.fitting.modelset.ModelManager;

public class StateModelManager implements ModelManager<StateModel> {

	@Override
	public StateModel createModelInstance() {
		// TODO Auto-generated method stub
		return new StateModel();
	}

	@Override
	public void copyModel(StateModel src, StateModel dst) {
		// TODO Auto-generated method stub
		dst.set(src);
	}

}
