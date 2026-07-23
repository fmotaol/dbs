package core.performer;

import java.util.concurrent.Future;

import core.Device;
import core.SavePoint;
import core.dataset.DataSet;
import core.events.Event;

public interface SlavePerformer extends Device {

	public Result execute(Context context);

	public void setAsDone();

	public void setAsDone(boolean done);

	public void showTree();

	public void notifyInvokerRequestedDataSet(SourcePerformer invoker, Context invokerContext,
			Future<DataSet> loadingDataSet);

	public void performDefaultStartImportingData(DataSet dataSet, Context invokerContext);

	public void performDefaultEndImportingData(DataSet dataSet, Context invokerContext);

//	public String getExecutionThreadId();

	public Event getEvent();

	public abstract SavePoint getExceptionCurrentSavePoint(Exception e);

	public void recursivelyGenerateImplicitSlaves();

	public void triggerAfterLastRowEvent(Context context);

	public void triggerBeforeFirstRowEvent(Context context);

	public void notifyInvokerJumpedNext(DataSet dataSet, Context invokerContext);

	public void resetRecordsFound();

	public boolean foundRecords();

}
