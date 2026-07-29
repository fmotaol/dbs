package core.dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.sql.Language;
import util.logical.Assert;

public class ColumnarDataSet extends AbstractDataSet {

	private HashMap<String, List<Object>> columns = new HashMap<>();
	
	private HashMap<String, String> fieldTypes = new HashMap<String, String>();

	private int currentRow = -1;
	
	public ColumnarDataSet() {
		super();
	}

	public void addColumn(String colName, List<Object> rows) {
		makeRowsCompatible(rows);
		columns.put(colName, rows);
		updateHeader();
	}

	private void makeRowsCompatible(List<Object> newColumnRows) {
		if (columns.isEmpty())
			return;
		while (newColumnRows.size() < totalRows()) {
			newColumnRows.add(null);
		}
			
		while (newColumnRows.size() > totalRows()) {
			addNullRow();
		}
	}

	public void addRow(Map<String, Object> record) {
		for (String f : columns.keySet()) {
			Object val = record.get(f);
			List<Object> values = (List<Object>) columns.get(f);
			if (values == null)
				throw new RuntimeException("Erro interno");
			values.add(val);
			String t = Language.inferType(val);
			updateFieldTypes(f, t);
		}
		updateHeader();
	}
	
	public void addNullRow() {
		for (String k : columns.keySet()) {
			List<Object> rows = columns.get(k);
			if (rows == null) {
				rows = new ArrayList<>();
				columns.put(k, rows);
			}
			rows.add(null);
		}
	}

	public void createEmptyColumns(Collection<String> colNames) {
		for (String f : colNames) {
			List<Object> valueList = columns.get(f);
			if (valueList == null) { // não existe esta coluna
				valueList = new ArrayList<>();
				columns.put(f, valueList);
			}
		}
	}

	public int totalRows() {
		if (columns.isEmpty())
			throw new RuntimeException("Não existem colunas");
		assertColumnsHaveSameSizeInRows();
		List<Object> values = columns.get(getFieldName(0));
		int sz = values.size();
		return sz;
	}

	private void assertColumnsHaveSameSizeInRows() {
		int prev = -1;
		for (String k : columns.keySet()) {
			List<Object> rows = columns.get(k);
			if (prev != -1) {
				Assert.that(prev == rows.size(), "Colunas possuem números diferentes de linhas");
			}
			prev = rows.size();
		}
	}

	@Override
	public Object readValue(int fieldIndex) {
		String field = getFieldName(fieldIndex);
		List<Object> values = columns.get(field);
		Object v = values.get(currentRow);
		return v;
	}

	@Override
	protected void putFields(Header header) {
		updateHeader(header);
	}

	private void updateHeader(Header header) {
		header.clear();
		for (String f : columns.keySet()) {
			header.addField(f, fieldTypes.get(f));
		}
	}

	private void updateHeader() {
		updateHeader(getHeader());
	}

	@Override
	public boolean hasNext() {
		int sz = totalRows();
		return currentRow < sz - 1;
	}

	@Override
	public boolean hasPrevious() {
		return currentRow >= 0;
	}

	@Override
	public void close() {
	}

	@Override
	public int getRowId() {
		return currentRow + 1;
	}

	@Override
	public boolean isActive() {
		return currentRow < totalRows();
	}

	@Override
	public boolean isBeforeFirst() {
		return currentRow == -1;
	}

	@Override
	protected boolean internalNext() {
		if (!hasNext())
			return false;
		currentRow++;
		updateRecordBuffers(false);
		return true;
	}

	private void updateFieldTypes(String field, String newObjType) {
		String t = fieldTypes.get(field);
		if (t == null)
			t = newObjType;
		else
			t = Language.defaultGeneralizeType(t, newObjType);
		fieldTypes.put(field, t);
	}

	public void reset() {
		currentRow = -1;
	}
	
}
