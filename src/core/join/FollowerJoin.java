package core.join;

import core.dataset.DataSet;
import core.performer.Context;
import core.performer.SourcePerformer;

public class FollowerJoin extends MatchJoin {

	public FollowerJoin(SourcePerformer lower) {
		super(lower);
	}

	private boolean newUpperRecord = false;

	@Override
	protected boolean internalNext(DataSet dataSet, Context invokerContext) {
		boolean r = next0(dataSet, invokerContext);
		if (!dataSet.hasNext() && !invokerContext.dataSet.hasNext()) //tanto o lower quanto o upper
			lower.freeDataSetInUse();
		newUpperRecord = false;
		return r;
	}

	protected boolean next0(DataSet dataSet, Context invokerContext) {

		if (lowerKey.isBeforeFirst())
			return lowerKey.next();

		if (newUpperRecord) {
			lowerKey.seek(upperKey.currentKey());
			return true;

//			if (upperKey.missedCurrentKeyIn(lowerKey)) {
//				return true; // pra forçar a executar os slaves do tipo miss, antes de efetivamente
//								// alternar para o novo registro do lower
//			}
		}

		int sd = lowerKey.commonSortDirection(upperKey);
		if (sd == 0)
			return lowerKey.next();
		else {
			int ck = lowerKey.compareCurrentKey(upperKey);
			if (ck * sd <= 0) // compara chaves quanto ao sentido
				return lowerKey.next();
			else
				return false;
		}
	}

	@Override
	protected boolean shouldPerformSlaves(Context context) {
		return true;
	}

	@Override
	public void newUpperRecord(DataSet dataSet, Context invokerContext) {
		newUpperRecord = true;
	}

}
