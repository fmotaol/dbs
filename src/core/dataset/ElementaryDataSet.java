package core.dataset;

public class ElementaryDataSet extends SingleRowDataSet {

//	private String[] fieldNames = { "field" };
//	private String[] fieldTypes = { "text" };
	private Object value;

	public ElementaryDataSet(Object value) {
		super();
		this.value = value;
	}

	public ElementaryDataSet(String fieldName, Object value) {
		super();
		getHeader().addField(fieldName, "text");
		this.value = value;
	}

	public ElementaryDataSet(String fieldName, String fieldType, Object value) {
		super();
		getHeader().addField(fieldName, fieldType);
		this.value = value;
	}

	@Override
	public int getFieldCount() {
		return 1;
	}

	@Override
	public void close() {
		// dir = null;
	}

	@Override
	public Object readValue(int fieldIndex) {

		if (fieldIndex != 0)
			throw new RuntimeException("Campo inexistente: " + fieldIndex);

		return value;
	}

	@Override
	protected void putFields(Header header) {
	}
}
