package core.dataset;

public class RecordBuffer extends Record {

//	private HashMap<Field, Object> fieldValues = new HashMap<Field, Object>();

	private Object[] values = new Object[0];

	public RecordBuffer(DataSet source, int rowId) {
		this(source, rowId, false);

	}

	public RecordBuffer(DataSet dataSet, int rowId, boolean nullValues) {
		super(dataSet, rowId);
		setHeader(dataSet.getHeader());

		if (nullValues)
			fillNullValues(dataSet);
		else {
			loadValues(dataSet);
//			ResultSetMetaData md = resultSet.getMetaData();
			//
//					for (int i = 1; i <= md.getColumnCount(); i++) {
//						String name = md.getColumnName(i);
//						String type = md.getColumnTypeName(i);
//						Object value = resultSet.getObject(name);
//						r.add(name, type, value);
//					}

//					TODO adicionar apenas os valores
		}
	}
	
	public RecordBuffer(DataSet dataSet, int rowId, Object[] values) {
		super(dataSet, rowId);
		setHeader(dataSet.getHeader());
		this.values = values;
	}

	private void loadValues(DataSet source) {
		values = source.readValues();
	}

	private void fillNullValues(DataSet source) {
		if (values.length == 0)
			values = new Object[source.getFieldCount()];
		for (int i = 0; i < values.length; i++) {
			values[i] = null;
		}
	}

	@Override
	public Object getValue(int fieldIndex) {
		return values[fieldIndex];
	}

	public void setValue(int fieldIndex, Object value) {
		values[fieldIndex] = value;
	}

//	public Field add(String name, String type, Object value) {
//		Field f = add(name, type);
//
//		setValue(f, value);
//
//		return f;
//	}

	@Override
	public boolean valid() {
		return true;
	}

}
