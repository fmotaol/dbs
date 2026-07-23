package core.performer;

import core.dataset.DataSet;
import core.jdbc.JDBCConnection;
import core.sql.Language;

public abstract class DBSConnection {

	public abstract String getId();

	public abstract DataSet query(String sql, Performer invoker);

	public abstract void setAutoCommit(Boolean autoCommit) throws Exception;

	public Result execute(String sql, Performer invoker) {
		return execute(sql, invoker, null);
	}

	public abstract Result execute(String sql, Performer invoker, Context context);

	public abstract Batch createBatch(Performer performer);

	@Deprecated
	public JDBCConnection getJDBCConnection() {
		throw new RuntimeException("Conex�o n�o � JDBC");
	}

	public abstract void defaultStartImportingData(TargetPerformer target);

	public abstract void defaultImportRow(TargetPerformer target, Context context);

	public abstract void defaultEndImportingData(TargetPerformer target);

	public abstract void reconnect();

	public abstract void close();

	public boolean hasNotice() {
		return false;
	}

	public String consumeNotice() {
		return null;
	}

	public Language getLanguage() {
		return Language.defaultLanguage();
	}

	public Result executeBatch(Batch batch) throws Exception {
		throw new RuntimeException("Execução de batch não suportada pela classe " + getClass());
	}

}
