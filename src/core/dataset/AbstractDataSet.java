package core.dataset;

import java.util.Arrays;

import core.performer.Context;

public abstract class AbstractDataSet implements DataSet {
	
	private InvokerData invokerData;

	protected Record currentRecord;

	protected Record previousRecord;

	private boolean buffered = true;

	private Header header;

	@Override
	public Header getHeader() {
		if (header == null) {
			loadHeader();
		}
		return header;
	}

	protected Header loadHeader() {
		header = new Header();
		putFields(header);
		return header;
	}

	public boolean isBuffered() {
		return buffered;
	}

	public void setBuffered(boolean buffered) {
		this.buffered = buffered;
	}

	public abstract boolean hasNext();

	public boolean previous() {
		throw new RuntimeException("Chamada previous() não permitida");
	}

	public boolean hasPrevious(){
		throw new RuntimeException("Chamada hasPrevious() não permitida");
	}

	public Record currentRecord() {
		return currentRecord;
	}

	public Record previousRecord() {
		return previousRecord;
	}

	public AbstractDataSet() {
		super();
	}

	public abstract void close();

	public abstract int getRowId();

	protected final void updateRecordBuffers(boolean nullThem) {

		if (nullThem)
			currentRecord = null;
		else {
			currentRecord = createCurrentRecord();
			if (buffered)
				currentRecord = createBufferedRecord();
		}

	}

	public final Record createCurrentRecord() {
		if (!isActive())
			return new InvalidRecord(this);
		
		// if (resultSet.next())
//			throw new RuntimeException("ResultSet está fechado"); TODO REVER

		DataSetRecord r = new DataSetRecord(this, getRowId());
		return r;
	}

	public abstract boolean isActive();

	public int getIndexOfFieldName(String name) {
		for (int i = 1; i <= getFieldCount(); i++) {
			String fn = getFieldName(i);
			if (fn.equalsIgnoreCase(name))
				return i;
		}
		throw new RuntimeException("Campo desconhecido: " + name);
	}

	public InvokerData getInvokerData() {
		return invokerData;
	}

	protected void setInvokerData(InvokerData invokerData) {
		this.invokerData = invokerData;
	}

	@Override
	public String toString() {
		InvokerData sd = getInvokerData();
		try {
			String r = Arrays.toString(getFieldNames());
			String alias = sd.getAlias();
			if (alias != null)
				r = alias + " " + r;
			return r;
		} catch (Exception e) {
			return "unable to retrieve DataSet.toString()";
		}
	}

	public abstract Object readValue(int fieldIndex);

	protected abstract void putFields(Header header);

	protected RecordBuffer createBufferedRecord() {
		return createBufferedRecord(getRowId());
	}

	protected RecordBuffer createBufferedRecord(int rowId) {
		RecordBuffer r = new RecordBuffer(this, rowId);
		return r;
	}

	@Deprecated
	public Record previewNextRecord() {
		throw new RuntimeException("Chamada previewNextRecord() não disponível");
	}

	public Object[] readValues() {
		Object[] r = new Object[getFieldCount()];
		for (int i = 0; i < r.length; i++) {
			r[i] = readValue(i);
		}
		return r;
	}

	public abstract boolean isBeforeFirst();

	public void beforeFirst() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	public boolean next() {
		try {

			previousRecord = currentRecord;
			
			return internalNext();

		} catch (Throwable e) {

			String ic = toString();
			if (ic != null)
				System.out.println("Erro no dataSet:\n" + ic);
			throw e;

		}
	}

	protected abstract boolean internalNext();

	public void setHeader(Header header) {
		this.header = header;
	}

	public void setInvokerData(String sql, Context context, String alias) {
		invokerData = new InvokerData(this);
		invokerData.setCommand(sql);
		invokerData.setContext(context);
		invokerData.setAlias(alias);
	}

	@Override
	public String getFieldName(int index) {
		Field f = getHeader().getField(index);
		return f.getName();
	}

	@Override
	public int getFieldCount() {
		return getHeader().getFieldCount();
	}


	@Override
	public String[] getFieldNames() {
		return getHeader().getFieldNames();
	}

}
