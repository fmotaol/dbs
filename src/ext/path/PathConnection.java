package ext.path;

import core.dataset.ColumnarDataSet;
import core.dataset.DataSet;
import core.performer.Batch;
import core.performer.Context;
import core.performer.DBSConnection;
import core.performer.Performer;
import core.performer.Result;
import core.performer.TargetPerformer;

public abstract class PathConnection extends DBSConnection {

	@Override
	public Result execute(String sql, Performer invoker, Context context) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public Batch createBatch(Performer performer) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void setAutoCommit(Boolean autoCommit) throws Exception {
	}

	@Override
	public void defaultStartImportingData(TargetPerformer target) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void defaultImportRow(TargetPerformer target, Context context) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void defaultEndImportingData(TargetPerformer target) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void reconnect() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void close() {
	}

	@Override
	public DataSet query(String sql, Performer invoker) {
		
		PathProcess p = newPathProcess(sql);
		p.parse();
		ColumnarDataSet r = p.getDataSet();
		return r;
	}

	protected abstract PathProcess newPathProcess(String sql);


}
