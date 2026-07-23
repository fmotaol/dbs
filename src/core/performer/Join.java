package core.performer;

import java.util.concurrent.Future;

import core.dataset.DataSet;

public abstract class Join {

	protected SourcePerformer lower;

	public Join(SourcePerformer lower) {
		super();
		this.lower = lower;
	}

	protected Future<DataSet> requestDataSet(String templateSQL, Context invokerContext) {
		String sql = lower.sqlConcretizer.concretizeAll(templateSQL, invokerContext);

		Future<DataSet> loadingDataSet;

		if (lower.asyncLoading)
			loadingDataSet = lower.asyncQueryDataSet(sql, lower, invokerContext);
		else
			loadingDataSet = lower.syncQueryDataSet(sql, lower, invokerContext);
		lower.notifySlavesDataSetRequested(lower, invokerContext, loadingDataSet);
		return loadingDataSet;
	}

	public void invokerRequestedDataSet(SourcePerformer invoker, Context invokerContext,
			Future<DataSet> loadingDataSet) {
		if (invoker != lower)
			return;
		for (SlavePerformer s : lower.getSlaves())
			s.notifyInvokerRequestedDataSet(invoker, invokerContext, loadingDataSet);
	}

	protected abstract boolean next(DataSet dataSet, Context invokerContext);

	protected void notifySlavesJumpedNext(DataSet dataSet, Context invokerContext) {
		for (SlavePerformer s : lower.getSlaves())
			s.notifyInvokerJumpedNext(dataSet, invokerContext);
	}

	public Context suitContext(Context context) {
		return context;
	}

	public boolean isUnderExecution() {
		DataSet d = lower.dataSetInUse();
		return (d != null);
	}

	public boolean finishedExecution() {
		return !isUnderExecution();
	}

	public boolean isNewExecution() {
		return !isUnderExecution();
	}

	protected abstract boolean shouldPerformSlaves(Context context);

	public void newUpperRecord(DataSet dataSet, Context invokerContext) {
		// nada
	}

	protected void reportCounter() {
		lower.reportCounter(true, true);
	}

	protected void incRecordCount(int inc) {
		lower.defaultIncRecordCount(inc);
	}

	public void resetRecordsFound() {
		lower.defaultResetRecordsFound();
	}

}
