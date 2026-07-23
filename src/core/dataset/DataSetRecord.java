package core.dataset;

public class DataSetRecord extends Record {
	
	public DataSetRecord(DataSet dataSet, int rowId) {
		super(dataSet, rowId);
		setHeader(dataSet.getHeader());
	}

	@Override
	public Object getValue(int fieldIndex) {
		if (!valid())
			throw new RuntimeException("Consulta de registro inválido (" + getRowId() + ")");

		return getDataSet().readValue(fieldIndex);
	}

	@Override
	public boolean valid() {
		return getRowId() == getDataSet().getRowId();
	}

	@Override
	public String toString() {
		if (valid())
			return super.toString();
		return "Registro inválido (" + getRowId() + ") em :" + getDataSet().getFieldNames();
	}

	@Override
	public String getAlias() {
		return getDataSet().getInvokerData().getAlias();
	}
}
