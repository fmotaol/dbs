package core.dataset;

import java.text.ParseException;

public interface DataTypeLanguage {
	
//	public Object getValue(String typeName, final Object value);

	public String valueAsNative(Object value);

	public String valueAsSQL(String typeName, final Object value);
	
	public Object convertValue(String value, String typeName) throws ParseException;

	public String generalizeType(String ta, String tb);

}
