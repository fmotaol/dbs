package core.performer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

import core.dataset.DataSet;
import core.dataset.Field;
import core.dataset.FieldValueSource;
import core.dataset.Record;
import core.sql.Language;
import util.Util;
import util.logical.Check;

public class Context implements FieldValueSource {
	
	public Context parent;

	public Record record;

	public DataSet dataSet;

	private Throwable exception;

	public Context(Context parent, Record record, DataSet dataSet) {
		super();
		this.parent = parent;
		this.record = record;
		this.dataSet = dataSet;
	}

	public Throwable getException() {
		if (exception == null)
			throw new RuntimeException("Não foi lançado erro");
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}

	public int getLevelOf(Record record) {
		if (record == this.record)
			return 1;
		if (parent == null)
			throw new RuntimeException("Inconsistência na cadeia de contextos");

		return parent.getLevelOf(record) + 1;
	}

	public String toString() {
		return toString(true);
	}

	public String toString(boolean deep) {
		String r = "dataSet=" + Check.coalesce(dataSet.getInvokerData().getAlias(), "?");
		if (deep)
			if (!deep)
				return r;
		r += "\n";
		if (parent != null)
			r += parent.toString(deep);
		return r;
	}

	public boolean printContextIdArray() {
		boolean shown = false;
		if (parent != null) {
			shown = parent.printContextIdArray();
		}
		int rowId = record.getRowId();
		if (rowId >= 0) {
			if (shown)
				System.out.print(".");

			System.out.print(rowId);
			return true;
		} else
			return shown;

	}

	private Record getOwnerRecord(Field field) {

		if (record.containsField(field.getName()))
			return record;

		if (parent == null)
			return null;

		Record r = parent.getOwnerRecord(field);
		return r;
	}

	public Field fieldByName(String fieldName, boolean upSearch) {
		fieldName = fieldName.toLowerCase();
		Field f = record.fieldByName(fieldName);
		if (f != null) {
			return f;
		} else {
			if (!upSearch)
				return null;

			if (parent != null)
				return parent.fieldByName(fieldName, upSearch);
			else
				return null;
		}
	}

	public Object getValue(Field field) {
		return getValue(field, true);
	}

	public Object getValue(Field field, boolean deep) {
		Record r;
		if (deep)
			r = getOwnerRecord(field);
		else
			r = record;
		if (r == null)
			throw new RuntimeException("Campo não encontrado: " + field);
		return r.getValue(field);
	}

	public Field getField(String fieldId) {
		return getField(fieldId, true);
	}

	public Field getField(String fieldId, boolean upSearch) {
		Field f = fieldByName(fieldId, upSearch);

		if (f == null) {
			if (Util.isInteger(fieldId)) {
				int i = Integer.parseInt(fieldId);
				f = record.getField(i);
			}
		}

		if (f == null)
			throw new RuntimeException("Campo não encontrado: " + fieldId);
		return f;
	}

	public Field getField(String fieldId, int level) {
		if (level == 0)
			return getField(fieldId, false);
		if (level > 0) {
			if (parent == null)
				throw new RuntimeException("Nível fora da faixa: " + level);
			return parent.getField(fieldId, level - 1);

		} else
			throw new RuntimeException("Nível inválido: " + level);
	}

	public Field fieldByName(String fieldName) {
		return fieldByName(fieldName, true);
	}

//	public void setParent(Record parent) {
//		this.parent = parent;
//		parent.child = this;
//	}
//
//	public Record getParent() {
//		return parent;
//	}

	public Field[] extractFieldsByNames(String[] fieldNames) {
		Field[] r = new Field[fieldNames.length];
		for (int i = 0; i < r.length; i++) {
			String name = fieldNames[i];
			Field f = getFieldByName(name);
			r[i] = f;
		}
		return r;
	}

	private Field getFieldByName(String name) {
		Field r = fieldByName(name);
		if (r == null)
			throw new RuntimeException("Campo não identificado: " + name);
		return r;
	}

	public String valueAsSQL(Field field, Language lang) {
		Object v = getValue(field);
		return lang.valueAsSQL(v);
	}

	public String sqlValueList(Field[] fields, String separator, Language lang, String[] fieldsToFilter) {
		return lang.sqlValueList(fields, separator, this, fieldsToFilter);
	}

	public Object getValue(String fieldName) {
		Field f = getField(fieldName);
		return getValue(f);
	}

	protected Field[] getDeepFields(Context context, boolean sortByLength) {
		Field[] r = record.getFields();
		r = Arrays.copyOf(r, r.length);

		if (parent != null) {
			Field[] p = parent.getDeepFields(sortByLength);
			Field[] s = Arrays.copyOf(r, r.length + p.length);
			for (int i = 0; i < p.length; i++) {
				s[r.length + i] = p[i];
			}
			r = s;
		}

		if (sortByLength)
			sortByNameLength(r);

		return r;
	}

	protected static void sortByNameLength(Field[] list) {
		Comparator<Field> c = new Comparator<Field>() {

			@Override
			public int compare(Field f1, Field f2) {
				if (f1.getName().length() < f2.getName().length())
					return 1;
				if (f1.getName().length() > f2.getName().length())
					return -1;
				if (f1.getName().length() == f2.getName().length())
					return 0;

				throw new RuntimeException("Condição não esperada");
			}
		};

		Arrays.sort(list, c);
	}

	protected Field[] deepFieldsOrderedByNameLength = null;

	public Field[] getDeepFieldsOrderedByNameLength(Context context) {
		if (deepFieldsOrderedByNameLength != null)
			return deepFieldsOrderedByNameLength;

		deepFieldsOrderedByNameLength = getDeepFields(context, true);

		return deepFieldsOrderedByNameLength;
	}

	public Field[] getDeepFields(boolean sortByLength) {
		return getDeepFields(this, sortByLength);
	}

	public Context getParent() {
		return parent;
	}

	public void setParent(Context parent) {
		this.parent = parent;
	}

	public Context nullValuesCopy() {
		Record r = record.nullValuesCopy();
		Context c = new Context(this.parent, r, dataSet);
		return c;
	}

//	public String valueAsString(Field field, boolean isNative, Language lang) {
//		Object value = getValue(field);
//		return lang.valueAsString(field, value, isNative);
//	}

	public String valueAsNative(Field field, Language language) {
		Object value = getValue(field);
		return language.valueAsNative(value);
	}

	public String stringValueAsSQL(Field field, Language language) {
		String value = valueAsNative(field, language);
		return language.stringValueAsSQL(value);
	}

	public String valueAsString(Field field, Language language, boolean forSQL) {
		Object v = getValue(field);
		String r = language.valueAsNative(v);
		if (forSQL)
			r = language.stringValueAsSQL(r);
		return r;
	}


	private HashMap<String, Record> savedRecords = new HashMap<String, Record>();

	public void saveRecord(String name, Record record) {
		name = name.toLowerCase();
		if (name.equals("prev"))
			throw new RuntimeException("Não é possível sobrescrever o registro \"prev\"");

		savedRecords.put(name, record);
	}

	public void saveRecordAsPrevious() {
		savedRecords.put("prev", record);
	}

	public Set<String> getSavedRecordNames(boolean b) {
		return savedRecords.keySet();
	}
	public Record previousRecord() {
		return savedRecords.get("prev");
	}

	private Record findSavedRecord(String recordName) {
		recordName = recordName.toLowerCase();
		return savedRecords.get(recordName);
	}

	public Record getRecord(String recordName) {
		recordName = recordName.toLowerCase();
		if (recordName.equals("current"))
			return record;

		Record r = findSavedRecord(recordName);
		if (r == null) {
			if (recordName.equals("prev"))
				throw new RuntimeException("Não existe registro anterior");
			else
				throw new RuntimeException("Não existe registro salvo como \"" + recordName + "\"");
		}

		return r;
	}

	public void saveCurrentRecord(String name) {
		saveRecord(name, record);
	}

}
