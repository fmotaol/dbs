package core.dataset;

public class ShiftedDataSet extends AbstractDataSet {
	
	private DataSet targetDataSet;
	
	private Record shiftedRecord;
	
	public ShiftedDataSet(DataSet targetDataSet) {
		super();
		this.targetDataSet = targetDataSet;
	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public int getFieldCount() {
		return targetDataSet.getFieldCount();
	}

	@Override
	public String getFieldName(int index) {
		return targetDataSet.getFieldName(index);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public int getRowId() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public String[] getFieldNames() {
		return targetDataSet.getFieldNames();
	}

	@Override
	public Object readValue(int fieldIndex) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	protected void putFields(Header header) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	protected boolean internalNext() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public boolean isBeforeFirst() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

}
