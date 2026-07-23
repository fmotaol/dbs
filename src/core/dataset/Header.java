package core.dataset;

import java.util.Arrays;
import java.util.HashMap;

public class Header {

	Field[] fields = new Field[0];

	HashMap<String, Field> fieldByName = new HashMap<String, Field>();

	private String[] fieldNames;

	public Header() {
		super();
	}

	public Field addField(String name, String type) {
		int index = fields.length;
		Field f = new Field(index, name, type);
		name = name.toLowerCase();
		fieldByName.put(name, f);
		Field[] fn = new Field[fields.length + 1];
		for (int i = 0; i < fields.length; i++) {
			fn[i] = fields[i];
		}
		fn[index] = f;
		fields = fn;

		return f;
	}

	public Field getField(int index) {
//		if ((index < 1) || (index > fields.length))
		if ((index < 0) || (index >= fields.length))
			throw new RuntimeException("Índice de campo inválido" + index);
		return fields[index];
//		return fields[index - 1];
	}

	public Field fieldByName(String fieldName) {
		return fieldByName.get(fieldName.toLowerCase());
	}

	public int getFieldCount() {
		return fields.length;
	}

	public boolean containsField(String fieldName) {
		Field f = fieldByName(fieldName);
		return f != null;
	}

	public String[] getFieldNames() {
		if (fieldNames == null) {
			fieldNames = new String[getFieldCount()];
			for (int i = 0; i < fields.length; i++) {
				String name = fields[i].getName();
				fieldNames[i] = name;
			}
		}

		return fieldNames;

	}

	public String bySeparator(String sep) {
		String r = "";
		for (Field f : fields) {
			if (!r.isEmpty())
				r += sep;
			r += f.getName();
		}
		return r;
	}

	@Override
	public String toString() {
		return "Header [" + Arrays.toString(fieldNames) + "]";
	}

	public void clear() {
		fields = new Field[] {};
		fieldNames = new String[] {};
		fieldByName.clear();
	}

}
