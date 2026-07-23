package core.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import core.dataset.AbstractDataSet;
import core.dataset.Header;
import core.dataset.Record;
import core.dataset.RecordBuffer;

public class JDBCDataSet extends AbstractDataSet {

	private int rowId = 0;

	private ResultSet resultSet;

	private Statement statement;

//	@Deprecated	
//	private String[] fieldNames = null;

	public JDBCDataSet(ResultSet resultSet, Statement statement) throws SQLException {
		super();
		this.resultSet = resultSet;
		this.statement = statement;
		loadHeader();
	}

	@Override
	protected synchronized boolean internalNext() {
		boolean r;

		try {
			if (isActive()) {

				r = resultSet.next();

			} else {

				if (statement != null && !statement.isClosed())
					statement.close();

				return false;
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		rowId++;

		updateRecordBuffers(!r);

		return r;
	}

	@Override
	public boolean hasNext() {
		try {
//			if (resultSet.isEmpty() || resultSet.isAfterLast())
//				
//				return false;
//			if (isActive())
//				return !resultSet.isLast();
//			else {
//				return false;
//			}

			boolean r = resultSet.next();
			if (r)
				resultSet.previous();

			return r;

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		if (resultSet == null)
			return;

		try {
			resultSet.getStatement().close();
			resultSet.close();
			resultSet = null;
		} catch (SQLException e) {
			System.out.println("Não foi possível fechar o resultSet");
			e.printStackTrace();
		}
	}

	@Override
	public boolean isActive() {
		if (resultSet == null)
			return false;
		try {
			
			if (resultSet.isClosed())
				return false;
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		// if (resultSet.isBeforeFirst())
		// return false;
		//
		// if (resultSet.isAfterLast())
		// return false;

		return true;
	}

	@Override
	public int getRowId() {
		return rowId;
	}

//	@Override
//	public Record currentRecord() throws SQLException {
////		if (currentRecord == null) {
////			updateRecordBuffers();
////		}
//		return currentRecord;
//	}

	@Override
	public Object readValue(int fieldIndex) {
		boolean a;
		a = isActive();

		if (!a)
			throw new RuntimeException("ResultSet não possui mais registro ativo");
		try {
			return resultSet.getObject(fieldIndex + 1);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void putFields(Header header) {
		try {
			ResultSetMetaData md = resultSet.getMetaData();
			for (int i = 1; i <= md.getColumnCount(); i++) {
				String name = md.getColumnName(i);
				String type = md.getColumnTypeName(i);
				header.addField(name, type);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void finalize() {
		try {
			statement.close();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized Record previewNextRecord() {
		RecordBuffer r;
		try {
			resultSet.next();
			int ri = rowId + 1;
			r = new RecordBuffer(this, ri);
			resultSet.previous();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return r;
	}

	@Override
	public boolean previous() {
		try {
			return internalPrevious();
		} catch (Throwable e) {
			String ic = toString();
			if (ic != null)
				System.out.println("Erro no dataSet:\n" + ic);
			throw new RuntimeException(e);
		}
	}

	private boolean internalPrevious() throws SQLException {
		boolean r;

		if (isActive()) {

			r = resultSet.previous();
		} else {
			if (statement != null && !statement.isClosed())
				statement.close();

			return false;
		}

		rowId--;

		updateRecordBuffers(!r);

		return r;
	}

	@Override
	public void beforeFirst() {
		try {
			internalBeforeFirst();
		} catch (Throwable e) {
			String ic = toString();
			if (ic != null)
				System.out.println("Erro no dataSet:\n" + ic);
			throw new RuntimeException(e);
		}
	}

	private void internalBeforeFirst() throws SQLException {

		if (isActive()) {

			resultSet.beforeFirst();
		} else {
			if (statement != null && !statement.isClosed())
				statement.close();

			return;
		}

		rowId = 0;

		updateRecordBuffers(true);

	}

//	@Override
//	public boolean hasPrevious() {
//		try {
//			boolean r = resultSet.previous();
//			if (r)
//				resultSet.next();
//
//			return r;
//
//		} catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
//	}

	@Override
	public boolean isBeforeFirst() {
		try {
			return resultSet.isBeforeFirst();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasPrevious() {
		try {
			return resultSet.getRow() > 1;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
