package core.join;

import core.dataset.DataSet;
import core.performer.Context;
import core.performer.Join;
import core.performer.SourcePerformer;

public class Cartesian extends Join {

	public Cartesian(SourcePerformer lower) {
		super(lower);
	}

	protected boolean next(DataSet dataSet, Context invokerContext) {
		if (!dataSet.hasNext())
			lower.freeDataSetInUse();
		boolean r = lower.defaultDataSetNext(dataSet);
		notifySlavesJumpedNext(dataSet, invokerContext);
		return r;
	}

	@Override
	protected boolean shouldPerformSlaves(Context context) {
		return true;
	}

}

