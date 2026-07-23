package core.jdbc;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import core.performer.Batch;
import core.performer.Performer;

@Deprecated
public class JDBCBatch extends Batch {

	private Statement statement;

	private JDBCConnection connection;

	public JDBCBatch(JDBCConnection connection) {
		super();
		this.connection = connection;
	}

	@Override
	public void add(String sql, Performer invoker) {
		super.add(sql, invoker);
		try {
			getStatement().addBatch(sql);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Deprecated
	protected int internalExecute_Old(List<String> buffer) throws SQLException {
		int[] is = getStatement(buffer).executeBatch();
		int r = 0;
		for (int i : is)
			if (i >= 0)
			r += i;
		return r;
	}

	Statement getStatement() throws SQLException {
		return getStatement(buffer);
	}

	private Statement getStatement(List<String> buffer) throws SQLException {
		try {
			if (isValid(statement))
				return statement;

		} catch (SQLException e) {
			System.out.println("Statement inválido. Recriando.");
		}

		createStatement(buffer);
		return statement;
	}

	@Deprecated
	private void createStatement(List<String> buffer) throws SQLException {
		statement = connection.createStatement();
		if (buffer != null)
			for (String sql : buffer) {
				statement.addBatch(sql);
			}
	}

	private boolean isValid(Statement statement) throws SQLException {
		if (statement == null)
			return false;
		try {

			if (statement.isClosed())
				return false;

//			if (!statement.getConnection().isValid(15000))
//				return false; 

		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	@Override
	public void close()  {
		if (statement != null)
			try {
				statement.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
	}

}
