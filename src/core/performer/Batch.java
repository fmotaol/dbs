package core.performer;

import java.util.ArrayList;
import java.util.List;

import core.savepoint.SavePoint;
import util.Util;

public class Batch {

	private int size = 0;
	protected ArrayList<String> buffer = new ArrayList<String>();
	private SavePoint savePoint;

	public int size() {
		return size;
	}

	public void add(String sql, Performer invoker) {
		sql = sql.trim();

		sql = prepareSQLforBatch(sql);

		buffer.add(sql);
		size++;
	}

	private String prepareSQLforBatch(String sql) {
//		Remove os ";" ao final, que são proibidos
//		TODO mover para dentro de Language (especialmente se isto for somente do Postgresql)

		while (sql.endsWith(";"))
			sql = Util.removeRight(sql, 1);
		return sql;
	}

	public synchronized Result execute(DBSConnection connection) throws Exception {
		Result r = internalExecute(buffer, connection);
		buffer.clear();
		return r;
	}
	
	protected Result internalExecute(List<String> buffer, DBSConnection connection) throws Exception {
		Result r = connection.executeBatch(this);
		return r;
	}

	public void close() {
		
	};

	@Override
	public String toString() {
		return getClass().getName() + " [size=" + size + "]";
	}

	public SavePoint getSavePoint() {
		return savePoint;
	}

	protected void setSavePoint(SavePoint savePoint) {
		this.savePoint = savePoint;
	}

	public List<String> getBuffer() {
		return buffer;
	}

}
