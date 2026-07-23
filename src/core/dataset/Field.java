package core.dataset;

import java.util.ArrayList;

public class Field implements Comparable<Field> {

	private int index;

	private String name;

	private String type;

	public Field(int index, final String name, final String type) {
		super();
		this.index = index;
		this.setName(name);
		this.setType(type);
	}

	public static Field[] removeByFieldName(final Field[] fields, final String[] fieldNamesToRemove) {
		ArrayList<Field> fs = new ArrayList<Field>();
		for (int i = 0; i < fields.length; i++) {
			Field f = fields[i];
			fs.add(f);
			for (int j = 0; j < fieldNamesToRemove.length; j++) {
				String n = fieldNamesToRemove[j];
				if (f.getName().equalsIgnoreCase(n))
					fs.remove(f);
			}
		}
		Field[] r = new Field[fs.size()];
		r = fs.toArray(r);
		return r;
	}

//	public Object getValue(Record record) {
//		
//		if (record.containsField(this))
//			return record.getValue(index);
//		else {
//			Record p = record.getParent();
//			if (p != null)
//				return getValue(p);
//		}
//			
//	}

	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	protected void setType(String type) {
		this.type = type;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public String toString() {
		return "Field [index=" + index + ", name=" + name + ", type=" + type + "]";
	}

	// FIXME, Bruno - alteração realizada em 28 de julho de 2017
	public int compareTo(Field outroField) {
		if (this.index < outroField.index) {
			return -1;
		}
		if (this.index > outroField.index) {
			return 1;
		}
		return 0;
	}
	
}
