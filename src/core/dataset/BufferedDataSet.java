package core.dataset;

import core.performer.Context;

public class BufferedDataSet implements DataSet {

	private DataSet source;

	private DataSetBuffer buffer;

	public DataSet getSource() {
		return source;
	}

	public BufferedDataSet(DataSet source) {
		super();
		this.source = source;
		buffer = new DataSetBuffer();
	}

	public int getIndexOfFieldName(String name) {
		return source.getIndexOfFieldName(name);
	}

	@Override
	public int getFieldCount() {
		if (sourceIsActive())
			return source.getFieldCount();
		else
			return buffer.getFieldCount();
	}

	@Override
	public String getFieldName(int index) {
		return source.getFieldName(index);
	}

	@Override
	public void close() {
		source.close();
		buffer.close();
	}

	@Override
	public int getRowId() {
		if (sourceIsActive())
			return source.getRowId();
		else
			return buffer.getRowId();
	}

	@Override
	public boolean isActive() {
		return sourceIsActive() || buffer.isActive();
	}

	private boolean sourceIsActive() {
		return source.isActive();
	}

	@Override
	public String[] getFieldNames() {
		if (sourceIsActive())
			return source.getFieldNames();
		else
			return buffer.getFieldNames();
	}

	@Override
	public Object readValue(int fieldIndex) {
		if (sourceIsActive())
			return source.readValue(fieldIndex);
		else
			return buffer.readValue(fieldIndex);
	}

	public void reset() {
		buffer.reset();
	}

//	@Override
//	public String getAlias() {
//		return source.getAlias();
//	}

	@Override
	public boolean next() {
		if (sourceIsActive()) {
			boolean r = source.next();
			Record cr = source.currentRecord();
			RecordBuffer b = cr.createBuffer();
			buffer.add(b);
			return r;
		} else {
			return buffer.next();
		}
	}

	@Override
	public boolean hasNext() {
		return buffer.hasNext();
	}

	@Override
	public boolean previous() {
		if (sourceIsActive()) {
			boolean r = source.previous();
			Record cr = source.currentRecord();
			RecordBuffer b = cr.createBuffer();
			buffer.add(b);
			return r;
		} else {
			return buffer.previous();
		}
	}

	@Override
	public boolean hasPrevious() {
		return buffer.hasPrevious();
	}

	@Override
	public boolean isBeforeFirst() {
		return buffer.isBeforeFirst();
	}

	@Override
	public Record currentRecord() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public Record previousRecord() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void beforeFirst() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public InvokerData getInvokerData() {
		return source.getInvokerData();
	}

	@Override
	public Header getHeader() {
		return source.getHeader();
	}

	@Override
	public void setInvokerData(String sql, Context context, String alias) {
		throw new RuntimeException("Chamada não permitida");
	}

}
