package core.join;

import core.dataset.DataSet;
import core.performer.Context;
import core.performer.SourcePerformer;

public class FilterJoin extends MatchJoin {

	public FilterJoin(SourcePerformer lower) {
		super(lower);
	}

	@Override
	protected boolean internalNext(DataSet dataSet, Context invokerContext) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	protected boolean shouldPerformSlaves(Context context) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

}
