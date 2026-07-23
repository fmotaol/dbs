package core.dataset;

import core.sql.Language;

public interface FieldValueSource {
	
	public Object getValue(Field field);

	public String valueAsNative(Field field, Language language);

	public String valueAsSQL(Field field, Language language);

}
