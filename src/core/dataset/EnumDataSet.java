package core.dataset;

import java.util.Arrays;

public class EnumDataSet<T> extends AbstractDataSet {

	private T[] list;
	
	private int cursor = -1;

	private String fieldName;
	
	public EnumDataSet(String fieldName, T[] list) {
		super();
		this.fieldName = fieldName;
		this.list = list;
		reset();
	}

	private void reset() {
		this.cursor = -1;
	}

	@Override
	public boolean internalNext() {
		if (cursor < list.length - 1) {
			currentRecord = null;
			cursor++;
			updateRecordBuffers(false);
			return true;
		} else
			return false;
	}

	@Override
	public boolean previous() {
		if (cursor > 0) {
			currentRecord = null;
			cursor--;
			updateRecordBuffers(false);
			return true;
		} else
			return false;
	}

	@Override
	public boolean hasNext() {
		return cursor < list.length - 1;
	}

	@Override
	public boolean hasPrevious() {
		return cursor > 0;
	}

	
	@Override
	public int getFieldCount() {
		return 1;
	}

	@Override
	public String getFieldName(int index) {
		if (index == 0)
			return fieldName;
		throw new RuntimeException("Índice de campo inválido");
	}

	@Override
	public void close() {
		cursor = list.length;
	}

	@Override
	public int getRowId() {
		return cursor + 1;
	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public String[] getFieldNames() {
		return new String[] { fieldName };
	}

	@Override
	public T readValue(int fieldIndex) {
		if (fieldIndex == 0)
			return list[cursor];
		throw new RuntimeException("Índice de campo inválido");
	}

	@Override
	protected void putFields(Header header) {
		if (header.getFieldCount() < 1)
			header.addField(fieldName, "unknown");
	}

	protected Record createRecord(int cursorPos)  {
		T[] vs = Arrays.copyOf(list, 1);
		vs[0] = list[cursorPos];
		RecordBuffer r = new RecordBuffer(this, cursorPos + 1, vs);
		return r;
	}

	@Override
	public Record previewNextRecord() {
		if (!hasNext())
			return null;
		Record r = createRecord(cursor + 1);
		return r;
	}

	
	@Override
	public boolean isBeforeFirst() {
		return cursor == -1;
	}

}
