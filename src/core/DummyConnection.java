package core;

import java.sql.SQLException;

import core.dataset.DataSet;
import core.dataset.ElementaryDataSet;
import core.performer.Batch;
import core.performer.Context;
import core.performer.DBSConnection;
import core.performer.Performer;
import core.performer.Result;
import core.performer.TargetPerformer;
import core.performer.VirtualBatch;
import core.sql.Language;

public class DummyConnection extends DBSConnection {

	@Override
	public Result executeBatch(Batch batch) throws SQLException {
		for (String s : batch.getBuffer()) {
			System.out.println(s);
		}
		return new Result(batch.size());
	}

	@Override
	public DataSet query(String sql, Performer invoker) {
		return new ElementaryDataSet("content", sql);
	}

	@Override
	public void setAutoCommit(Boolean autoCommit) throws SQLException {
	}

	@Override
	public Result execute(String sql, Performer invoker, Context context) {
		return new Result(0);
	}

	@Override
	public Batch createBatch(Performer performer) {
		return new VirtualBatch(this, performer);
	}

	@Override
	public void defaultStartImportingData(TargetPerformer target) {
	}

	@Override
	public void defaultImportRow(TargetPerformer target, Context context) {
	}

	@Override
	public void defaultEndImportingData(TargetPerformer target) {
	}

	@Override
	public void reconnect() {
	}

	@Override
	public void close() {
	}

	@Override
	public Language getLanguage() {
		return Language.defaultLanguage();
	}

	@Override
	public String getId() {
		return "dummy";
	}

}
