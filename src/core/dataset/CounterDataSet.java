package core.dataset;

public class CounterDataSet extends AbstractDataSet {

	private String fieldName;

	private int rowId = 0;

	private int start;

	private int end;

	private Integer step = 1;

	private int cursor;

	public Integer getStep() {
		return step;
	}

	public void setStep(Integer step) {
		this.step = step;
	}

	public CounterDataSet(String fieldName, int start, int end) {
		super();
		this.fieldName = fieldName;
		this.start = start;
		this.end = end;
		reset();
	}

	public CounterDataSet(String fieldName, int start, int end, int step) {
		this(fieldName, start, end);
		this.step = step;
	}

	private void reset() {
		this.cursor = start - step();
		this.rowId = -1;
	}

	@Override
	public boolean internalNext() {
		int step = step();
		if (cursor < end) {
			currentRecord = null;
			cursor += step;
			rowId++;
			updateRecordBuffers(false);
			return true;
		} else
			return false;
	}

	@Override
	public boolean previous() {
		int step = step();
		if (cursor > start) {
			currentRecord = null;
			cursor -= step;
			rowId--;
			return true;
		} else
			return false;
	}
	
	@Override
	public Record previewNextRecord() {
		int step = step();
		if (cursor + step > end)
			throw new RuntimeException("Fim da contagem");
		Object[] vs = new Object[1];
		vs[0] = cursor + step;
		RecordBuffer r = new RecordBuffer(this, rowId + 1, vs);
		return r;
	}

//	TODO parece que nÃ£o admite contagem inversa! Veja esse sinal de < a seguir, por ex.

	@Override
	public boolean hasNext() {
		if (cursor < end) {
			return true;
		} else
			return false;
	}

	@Override
	public boolean hasPrevious() {
		if (cursor > start) {
			return true;
		} else
			return false;
	}

	public int step() {
		int step;
		if (this.step == null) {
			if (end < start)
				step = -1;
			else
				step = 1;
		} else
			step = this.step;
		return step;
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
		cursor = end + step();
		rowId = -1;
	}

	@Override
	public int getRowId() {
		return rowId;
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
	public Object readValue(int fieldIndex) {
		if (fieldIndex == 0)
			return cursor;
		throw new RuntimeException("Índice de campo inválido");
	}

	@Override
	protected void putFields(Header header) {
		if (header.getFieldCount() < 1)
			header.addField(fieldName, "int");
	}

	@Override
	public boolean isBeforeFirst() {
		return rowId == 0;
	}

}
