package core.performer;

import java.util.List;

public class VirtualBatch extends Batch {

	private DBSConnection connection;
	private int affectedRows = 0;

	@Override
	protected Result internalExecute(List<String> buffer, DBSConnection connection) {
		return new Result(affectedRows);
	}

	@Override
	public void close() {
		// nada
	}

	public VirtualBatch(DBSConnection connection, Performer performer) {
		super();
		this.connection = connection;
		performer.warning("Criado batch virtual para processamento imediato.");
	}

	@Override
	public void add(String sql, Performer invoker) {
		super.add(sql, invoker);
		invoker.logSQL(connection, sql);
		Result r = connection.execute(sql, invoker);
		invoker.logStatusSQLResult(connection, r);
		affectedRows += r.getAffectedRows();
	}

}
