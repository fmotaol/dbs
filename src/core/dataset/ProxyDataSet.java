package core.dataset;

import core.performer.Context;

public abstract class ProxyDataSet implements DataSet {
	
	protected DataSet target;

	public ProxyDataSet(DataSet target) {
		super();
		this.target = target;
	}

	public boolean next() {
		return target.next();
	}

	public boolean hasNext() {
		return target.hasNext();
	}

	public boolean previous() {
		return target.previous();
	}

	public boolean hasPrevious() {
		return target.hasPrevious();
	}

	public Record currentRecord() {
		return target.currentRecord();
	}

	public int getFieldCount() {
		return target.getFieldCount();
	}

	public String getFieldName(int index) {
		return target.getFieldName(index);
	}

	public void close() {
		target.close();
	}

	public int getRowId() {
		return target.getRowId();
	}

	public Record previousRecord() {
		return target.previousRecord();
	}

	public boolean isActive() {
		return target.isActive();
	}

	public String[] getFieldNames() {
		return target.getFieldNames();
	}

	public int getIndexOfFieldName(String name) {
		return target.getIndexOfFieldName(name);
	}

	public Object readValue(int fieldIndex) {
		return target.readValue(fieldIndex);
	}

	public Object[] readValues() {
		return target.readValues();
	}

	public boolean isBeforeFirst() {
		return target.isBeforeFirst();
	}

	public void beforeFirst() {
		target.beforeFirst();
	}

	public InvokerData getInvokerData() {
		return target.getInvokerData();
	}

	public Header getHeader() {
		return target.getHeader();
	}

	public void setInvokerData(String sql, Context context, String alias) {
		target.setInvokerData(sql, context, alias);
	}


}
