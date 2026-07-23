package core.dataset;

import java.util.ArrayList;

public class DataSetBuffer extends AbstractDataSet {

	private ArrayList<RecordBuffer> records = new ArrayList<RecordBuffer>();

	private int cursor = 0;

	public void add(RecordBuffer record) {
		checkFields(record);
		records.add(record);
	}

	private void checkFields(RecordBuffer record) {
		if (record.getFieldCount() != getHeader().getFieldCount())
			throw new RuntimeException("Record incompatível com DataSet");
	}

	public void reset() {
		cursor = 0;
	}

	@Override
	public boolean internalNext() {
		if (cursor == -1)
			throw new RuntimeException("DataSet está fechado");
		if (!hasNext())
			return false;
		cursor++;
		return true;
	}

	@Override
	public boolean previous() {
		if (cursor == -1)
			throw new RuntimeException("DataSet está fechado");
		if (!hasPrevious())
			return false;
		cursor--;
		return true;
	}

	public boolean hasNext() {
		return cursor > 0 && cursor < records.size();
	}

	@Override
	public boolean hasPrevious() {
		return cursor > 0; // && cursor < records.size();
	}
	
	@Override
	public int getFieldCount() {
		return getHeader().getFieldCount();
	}

	@Override
	public String getFieldName(int index) {
		return getHeader().getFieldNames()[index];
	}

	@Override
	public void close() {
		cursor = -1;
	}

	@Override
	public int getRowId() {
		return cursor;
	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub

		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public String[] getFieldNames() {
		return getHeader().getFieldNames();
	}

	@Override
	public Object readValue(int fieldIndex) {
		Record r = getCurrentRecord();
		return r.getValue(fieldIndex);
	}

	private RecordBuffer getCurrentRecord() {
		if (cursor == -1)
			throw new RuntimeException("DataSet está fechado");
		return records.get(cursor);
	}

	@Override
	protected void putFields(Header header) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	public void loadAll() {
		throw new RuntimeException("ainda não implementado");		
	}

	@Override
	public Record previewNextRecord() {
		next();
		RecordBuffer r = new RecordBuffer(this, cursor);
		previous();
		return r;
	}

	@Override
	public boolean isBeforeFirst() {
		return cursor == -1;
	}

	
}
