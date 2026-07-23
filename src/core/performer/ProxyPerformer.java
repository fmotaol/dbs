package core.performer;

import java.util.concurrent.Future;

import core.Device;
import core.SavePoint;
import core.dataset.DataSet;
import core.events.Event;

public abstract class ProxyPerformer implements SlavePerformer {

	private SlavePerformer proxy;

	public ProxyPerformer(SlavePerformer proxy) {
		this.proxy = proxy;
	}

	public SlavePerformer getProxy() {
		return proxy;
	}

	public String getName() {
		return proxy.getName();
	}

	public String getSimpleName() {
		return proxy.getSimpleName();
	}

	public String getFullName() {
		return proxy.getFullName();
	}

	public Device findDeviceRel(String relName) {
		return proxy.findDeviceRel(relName);
	}

//	public void execute(DataSet source, Record invokerRecord) throws SQLException, IOException, ParseException {
//		proxy.execute(source, invokerRecord);
//	}

	public void setAsDone() {
		proxy.setAsDone();
	}

	public void setAsDone(boolean done) {
		proxy.setAsDone(done);
	}

	public void showTree() {
		proxy.showTree();
	}

	public void notifyInvokerRequestedDataSet(SourcePerformer invoker, Context invokerContext,
			Future<DataSet> loadingDataSet) {
		proxy.notifyInvokerRequestedDataSet(invoker, invokerContext, loadingDataSet);
	}

	@Override
	public void performDefaultStartImportingData(DataSet dataSet, Context context) {
		proxy.performDefaultStartImportingData(dataSet, context);
	}

	@Override
	public void performDefaultEndImportingData(DataSet dataSet, Context context) {
		proxy.performDefaultEndImportingData(dataSet, context);
	}

//	public String getExecutionThreadId() {
//		return proxy.getExecutionThreadId();
//	}

	public Event getEvent() {
		return proxy.getEvent();
	}

	public SavePoint getExceptionCurrentSavePoint(Exception e) {
		return proxy.getExceptionCurrentSavePoint(e);
	}

	public void recursivelyGenerateImplicitSlaves() {
		proxy.recursivelyGenerateImplicitSlaves();
	}

	public void triggerAfterLastRowEvent(Context context) {
		proxy.triggerAfterLastRowEvent(context);
	}

	public void triggerBeforeFirstRowEvent(Context context) {
		proxy.triggerBeforeFirstRowEvent(context);
	}

}
