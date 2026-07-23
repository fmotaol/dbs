package core.dataset;

import java.sql.SQLException;
import java.util.Collection;

import core.sql.Language;
import util.Util;

public abstract class Record implements FieldValueSource {

//	@Deprecated
//	private Record parent;

	@Deprecated
	private Record child;

	private Header header;

	private int rowId;

	private DataSet dataSet;

	public Record(DataSet source, int rowId) {
		super();
		this.dataSet = source;
		this.rowId = rowId;
	}

	public Field getFieldByName(String name) {
		Field r = fieldByName(name);
		if (r == null)
			throw new RuntimeException("Campo não identificado: " + name);
		return r;
	}

//	public String getType(String fieldName) {
//		Field f = getField(fieldName);
//		return f.getType();
//	}

	public String getValueAsString(String fieldName) {
		Object value = getValue(fieldName);
		String sv = null;
		if (value != null)
			sv = value.toString();
		else
			sv = "null";
		return sv;
	}

	public Object getValue(Field field) {
		int i = field.getIndex();
		return getValue(i);
	}

	public Object getValue(String fieldName) {
		Field f = getField(fieldName);
		return getValue(f);
	}

	public abstract Object getValue(int fieldIndex);

	public Field fieldByName(String fieldName) {
		return header.fieldByName(fieldName);
	}

	String[] toArray(Collection<String> col) {
		String[] r = new String[col.size()];
		int i = 0;
		for (String s : col) {
			r[i] = s;
			i++;
		}
		return r;
	}

	@Override
	public String toString() {
		StringBuilder r = new StringBuilder();

		for (Field f : header.fields) {
			if (r.length() > 0)
				r.append(", ");
			else {
				String a = getAlias();
				if (a == null)
					a = "";
				r.append("Record " + a + "[");
			}
			Object v = getValue(f);
			String value = "null";
			if (v != null)
				value = v.toString();
//			if ((value != null) && (value.length() > 40))
			if (value.length() > 40)
				value = value.substring(0, 40) + "(...)";
			r.append(f.getName() + "=" + value);
		}
		r.append("]");
		String s = r.toString();
		r.setLength(0);
		r.trimToSize();
		return s;
	}

	public int getRowId() {
		return rowId;
	}

//	public Field[] getFields() {
//		return header.getFields(false);
//	}

	public String getFieldName(int index) throws SQLException {
		return dataSet.getFieldName(index);
	}

	// public abstract Object getValue(Field field);

	// public abstract void setValue(Field field, Object value);

	public DataSet getDataSet() {
		return dataSet;
	}

	public abstract boolean valid();

	public Header getHeader() {
		if (header == null)
			header = new Header();
		return header;
	}

	public void setHeader(Header header) {
		this.header = header;
	}

	public Field[] getFields() {
		return header.fields;
	}

	public String valueAsString(Field field, boolean isNative, Language language) {
		Object value = getValue(field);
		return language.valueAsString(field, value, isNative);
	}

	public String valueAsNative(Field field, Language language) {
		Object value = getValue(field);
		return language.valueAsNative(value);
	}

	public int getFieldCount() {
		return header.getFieldCount();
	}

	public RecordBuffer createBuffer() {
		return new RecordBuffer(dataSet, rowId);
	}

	public String valueAsSQL(Field field, Language language) {
		String value = valueAsNative(field, language);
		return language.stringValueAsSQL(value);
	}

	public String getAlias() {
		return dataSet.getInvokerData().getAlias();
	}

	public String bySeparator(String sep) {
		String r = "";
		for (Field f : getFields()) {
			if (!r.isEmpty())
				r += sep;
			r += getValueAsString(f.getName());
		}
		return r;
	}

	public String bySeparator(String sep, boolean forSQL) {
		String r = "";
		for (Field f : getFields()) {
			if (!r.isEmpty())
				r += sep;
			r += getValueAsString(f.getName());
		}
		return r;
	}

	public Record nullValuesCopy() {
		RecordBuffer r = new RecordBuffer(dataSet, rowId, true);
		return r;
	}

	public boolean containsField(String name) {
		return header.containsField(name);
	}

	public Field getField(int index) {
		return header.getField(index);
	}

	public Field getField(String fieldId) {
		Field r = fieldByName(fieldId);
		if (r == null) {
			if (Util.isInteger(fieldId)) {
				int i = Integer.parseInt(fieldId);
				r = getField(i);
			}
		}

		if (r == null)
			throw new RuntimeException("Campo não encontrado: " + fieldId);
		return r;
	}

	public String valueAsString(Field field, Language lang, boolean forSQL) {
		Object v = getValue(field);
		String r = lang.valueAsNative(v);
		if (forSQL)
			r = lang.stringValueAsSQL(r);
		return r;
	}

}
