package core.sql;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.postgresql.jdbc.PgArray;

import core.dataset.DataTypeLanguage;
import core.dataset.Field;
import core.dataset.FieldValueSource;
import util.Strings;
import util.Util;

public abstract class Language implements DataTypeLanguage {

	static final String DEFAULT_INSERT_TEMPLATE = "insert into @#tablename (@#fieldnames) values (@*);";
	static final String DEFAULT_UPDATE_TEMPLATE = "update @#tablename set @*=-pk where @*=pk;";
	static final boolean REPLACE_COMMA_WITH_DECIMAL = true;
	
//	private FieldValueSource aditionalValue

	public String getInsertTemplate() {
		return DEFAULT_INSERT_TEMPLATE;
	}

	public String getUpdateTemplate() {
		return DEFAULT_UPDATE_TEMPLATE;
	}

	private static Language defaultLanguage = new DefaultLanguage();

	public static Language defaultLanguage() {
		return defaultLanguage;
	}

	public String getUpsertTemplate() {
		return null;
	}

	public String valueAsSQL(String typeName, final Object value) {
		if (value == null)
			return "null";
		typeName = typeName.toLowerCase();

		if (typeName.equals("int"))
			return value.toString();
		if (typeName.equals("integer"))
			return value.toString();
		if (typeName.equals("int2"))
			return value.toString();
		if (typeName.equals("int4"))
			return value.toString();
		if (typeName.equals("int8"))
			return value.toString();
		if (typeName.equals("smallint"))
			return value.toString();
		if (typeName.equals("short"))
			return value.toString();
		if (typeName.equals("double"))
			return treatNumber(value);
		if (typeName.equals("float8"))
			return treatNumber(value);
		if (typeName.equals("float4"))
			return treatNumber(value);
		if (typeName.equals("float"))
			return treatNumber(value);
		if (typeName.equals("real"))
			return treatNumber(value);
		if (typeName.equals("long"))
			return value.toString();
		if (typeName.equals("bigint"))
			return value.toString();
		if (typeName.equals("number"))
			return treatNumber(value);
		if (typeName.equals("numeric"))
			return treatNumber(value);
		if (typeName.equals("decimal"))
			return treatNumber(value);
		if (typeName.equals("boolean"))
			return value.toString();
		if (typeName.equals("bool"))
			return value.toString();
		if (typeName.equals("char"))
			return stringValueAsSQL((String) value);
		if (typeName.equals("bpchar"))
			return stringValueAsSQL((String) value);
		if (typeName.equals("varchar2"))
			return stringValueAsSQL((String) value);
		if (typeName.equals("varchar"))
			return stringValueAsSQL((String) value);
		if (typeName.equals("unknown"))
			return stringValueAsSQL(String.valueOf(value));
		if (typeName.equals("text"))
			return stringValueAsSQL((String.valueOf(value)));
		if (typeName.equals("string"))
			return stringValueAsSQL((String.valueOf(value)));
		if (typeName.equals("timestamp"))
			return "'" + value + "'";
		if (typeName.equals("timestamptz"))
//			return Util.concat("'", timestampFormat((Timestamp) value), "'");
			return "'" + value + "'";
		if (typeName.equals("date"))
			return Util.concat("'", dateFormat((Date) value), "'");

		// arrays
		if (typeName.startsWith("_"))
			return stringValueAsSQL(value.toString());

		throw new RuntimeException("Tipo n�o suportado: " + typeName);
	}

	private String treatNumber(final Object value) {
		if (REPLACE_COMMA_WITH_DECIMAL)
			return value.toString().replace(",", ".");
		return value.toString();
	}

	public String valueAsSQL(final Object value) {
		if (value == null)
			return "null";

		if (value instanceof Field)
			throw new RuntimeException("Erro interno");

		if (value instanceof Integer)
			return value.toString();
		if (value instanceof Long)
			return value.toString();
		if (value instanceof Short)
			return value.toString();
		if (value instanceof Double)
			return treatNumber(value);
		if (value instanceof Number)
			return treatNumber(value);
		if (value instanceof Float)
			return treatNumber(value);
		if (value instanceof Boolean)
			return value.toString();
		if (value instanceof String)
			return stringValueAsSQL((String) value);
		if (value instanceof Timestamp)
//			return Util.concat("'", timestampFormat((Timestamp) value), "'");
			return Util.concat("'", value.toString(), "'");
		if (value instanceof Date)
			return Util.concat("'", timestampFormat((Date) value), "'");

		return valueAsSQLForSpecialTypes(value);
	}

	protected String valueAsSQLForSpecialTypes(Object value) {
		if (value instanceof PgArray) {
			try {
				Object[] es = (Object[]) ((PgArray) value).getArray();
				String[] ss = new String[es.length];
				for (int i = 0; i < es.length; i++) {
					ss[i] = valueAsSQL(es[i]);
				}

				String s = Strings.concat(ss, ", ");
				return "array[" + s + "]";
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		throw new RuntimeException("Tipo n�o suportado: " + value.getClass().getName());
	}

	public String stringValueAsSQL(String value) {
//		value = value.replace("\\", "\\\\"); // tem que ser nesta ordem, sen�o ele duplica os "\"
		value = value.replace("'", "''");
		return Util.concat("'", value, "'");
	}

	private static final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSz");

	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSSz");

	protected String timestampFormat(Date date) {
		return timestampFormat.format(date);
	}

	protected String timestampFormat(Timestamp timestamp) {
		return timestampFormat.format(timestamp);
	}

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public String dateFormat(final Date date) {
		return dateFormat.format(date);
	}

	protected String arrayAsSQL(Object[] ar) {
		String s = "";
		for (Object a : ar) {
			if (!"".equals(s))
				s += ", ";
			s += a;
		}
		String r = "'{" + s + "}'";
		return r;
	}

	public String valueAsNative(Object value) {
		return defaultValueAsNative(value);
	}

	public static String defaultValueAsNative(Object value) {
		if (value != null)
			return value.toString();
		else
			return "";
	}

	public String valueAsString(Field field, Object value, boolean isNative) {
		if (isNative)
			return valueAsNative(value);
		else
			return valueAsSQL(value);
	}

	public String inferTypeFromText(String value) {

		if (value == null)
			return "unknown";

		String t = inferNumericType(value);
		if (t != null)
			return t;

		t = inferBooleanType(value);
		if (t != null)
			return t;

		t = inferDateTimeType(value);
		if (t != null)
			return t;

		return "unknown";
	}

	public static String inferType(Object v) {

		if (v == null)
			return "unknown";

		if (v instanceof Integer)
			return "int";
		if (v instanceof Short)
			return "short";
		if (v instanceof Double)
			return "double";
		if (v instanceof Float)
			return "float";
		if (v instanceof Long)
			return "long";
		if (v instanceof Number)
			return "number";
		if (v instanceof BigDecimal)
			return "decimal";
		if (v instanceof Boolean)
			return "boolean";
		if (v instanceof Character)
			return "char";
		if (v instanceof String)
			return "string";
		if (v instanceof Date)
			return "timestamp";
		if (v instanceof Timestamp)
			return "timestamp";

		throw new RuntimeException("Tipo de dados n�o identificado para : " + v.getClass());
	}

	public String inferDateTimeType(String value) {
		try {
			defaultDateFormat.parse(value);
			return "date";
		} catch (Exception e) {
		}

		try {
			timestampFormat.parse(value);
			return "timestamp";
		} catch (Exception e) {
		}

		try {
			timeFormat.parse(value);
			return "time";
		} catch (Exception e) {
		}

		return null;
	}

	private String inferBooleanType(String value) {
		try {
			Boolean.parseBoolean(value);
			return "boolean";
		} catch (Exception e) {
		}

		return null;
	}

	protected String inferNumericType(String value) {
		try {
			Short.parseShort(value);
			return "short";
		} catch (NumberFormatException e) {
		}

		try {
			Integer.parseInt(value);
			return "int";
		} catch (NumberFormatException e) {
		}

		try {
			Float.parseFloat(value);
			return "float";
		} catch (NumberFormatException e) {
		}

		try {
			Long.parseLong(value);
			return "long";
		} catch (NumberFormatException e) {
		}

		try {
			Double.parseDouble(value);
			return "double";
		} catch (NumberFormatException e) {
		}

		return null;
	}

	public String[] inferTypesFromText(String[] values) {
		String[] r = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			r[i] = inferTypeFromText(values[i]);
		}
		return r;
	}

	private static final SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	protected Date parseDate(String value) throws ParseException {
		return defaultDateFormat.parse(value);
	}

	public Object convertValue(String value, String typeName) throws ParseException {
		if (value == null)
			return null;
		if (value.equals("null"))
			return null;
		typeName = typeName.toLowerCase();

		if (typeName.equals("int"))
			return Integer.parseInt(value);
		if (typeName.equals("integer"))
			return Integer.parseInt(value);
		if (typeName.equals("int2"))
			return Short.parseShort(value);
		if (typeName.equals("int4"))
			return Integer.parseInt(value);
		if (typeName.equals("int8"))
			return Long.parseLong(value);
		if (typeName.equals("smallint"))
			return Short.parseShort(value);
		if (typeName.equals("short"))
			return Short.parseShort(value);
		if (typeName.equals("double"))
			return Double.parseDouble(value);
		if (typeName.equals("float8"))
			return Double.parseDouble(value);
		if (typeName.equals("float4"))
			return Double.parseDouble(value);
		if (typeName.equals("float"))
			return Double.parseDouble(value);
		if (typeName.equals("real"))
			return Float.parseFloat(value);
		if (typeName.equals("long"))
			return Long.parseLong(value);
		if (typeName.equals("bigint"))
			return Long.parseLong(value);
		if (typeName.equals("number"))
			return Double.parseDouble(value);
		if (typeName.equals("numeric"))
			return Double.parseDouble(value);
		if (typeName.equals("decimal"))
			return Double.parseDouble(value);
		if (typeName.equals("float8"))
			return Double.parseDouble(value);
		if (typeName.equals("boolean"))
			return Boolean.parseBoolean(value);
		if (typeName.equals("bool"))
			return Boolean.parseBoolean(value);
		if (typeName.equals("char"))
			return value;
		if (typeName.equals("bpchar"))
			return value;
		if (typeName.equals("varchar2"))
			return value;
		if (typeName.equals("varchar"))
			return value;
		if (typeName.equals("unknown"))
			return value;
		if (typeName.equals("text"))
			return value;
		if (typeName.equals("string"))
			return value;
		if (typeName.equals("timestamp"))
			return parseTimestamp(value);
		if (typeName.equals("timestamptz"))
			return parseTimestamp(value);
		if (typeName.equals("date"))
			return parseDate(value);

		// arrays
		if (typeName.startsWith("_"))
			return value;

		throw new RuntimeException("Tipo n�o suportado: " + typeName);
	}

	protected Object parseTimestamp(String value) {
		return value;
		// throw new RuntimeException("ainda n�o implementado");
		// TODO Auto-generated method stub
	}

	public String simpleValue(Object value) {
		if (value == null)
			return "null";
		if (value instanceof Date)
			return timestampFormat((Date) value);
		return value.toString();
	}

	public String sqlValueList(Field[] fields, String separator, FieldValueSource source, String[] fieldsToFilter) {
		StringBuilder r = new StringBuilder();
		for (Field f : fields) {
			if (fieldsToFilter != null)
				if (!Util.containsIgnoreCase(fieldsToFilter, f.getName()))
					continue;

			Object v = source.getValue(f);
			String sv = valueAsSQL(v);

			if (r.length() > 0)
				r.append(separator);

			r.append(sv);
		}
		String s = r.toString();
		r.setLength(0);
		r.trimToSize();
		return s;
	}
	
	public String generalizeType(String ta, String tb) {
		return defaultGeneralizeType(ta, tb);
	}

	public static String defaultGeneralizeType(String ta, String tb) {
		ta = convertTypeIntoDefaultSynonim(ta);
		tb = convertTypeIntoDefaultSynonim(tb);

		if ((ta == null) && (tb == null))
			return null;

		if ((ta == null) || (tb == null))
			return Util.ifNull(ta, tb);

		if (match(ta, tb))
			return ta.toLowerCase();

		if (match(ta, "unknown") || match(tb, "unknown"))
			return "unknown";

		if (match(ta, "double") && isNumericType(tb))
			return "double";

		if (match(ta, tb, "short", "int"))
			return "int";

		if (match(ta, tb, "short", "int"))
			return "int";

		if (match(ta, tb, "int", "long"))
			return "long";

		if (match(ta, tb, "short", "long"))
			return "long";

		if (match(ta, tb, "short", "real"))
			return "real";

		if (match(ta, tb, "int", "real"))
			return "real";

		if (match(ta, tb, "long", "real"))
			return "double";

		if (isNumericType(ta) && isNumericType(tb) && !match(ta, tb))
			return "double";

		if (match(ta, "string") || match(tb, "string"))
			return "string";

		if (matchAny(ta, "date", "time", "timestamp") && matchAny(tb, "date", "time", "timestamp") && !match(ta, tb))
			return "timestamp";

		/*
		 * if (isNumericType(ta) && !isNumericType(tb)) return "unknown";
		 * 
		 * if (isNumericType(tb) && !isNumericType(ta)) return "unknown";
		 * 
		 * if (match(ta, "boolean") && !match(tb, "boolean")) return "unknown";
		 * 
		 * if (match(tb, "boolean") && !match(ta, "boolean")) return "unknown";
		 */

		return "unknown";

	}

	public String[] generalizeTypes(String[] a, String[] b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		if (a.length != b.length)
			throw new RuntimeException("Arrays de tipos com tamanhos diferentes");
		String[] r = new String[a.length];
		for (int i = 0; i < r.length; i++) {
			r[i] = generalizeType(a[i], b[i]);
		}
		return r;
	}

	protected static String convertTypeIntoDefaultSynonim(String typeName) {
		if (typeName == null)
			return null;

		typeName = typeName.toLowerCase();

		if (typeName.equals("null"))
			return null;

		if (typeName.equals("int"))
			return "int";
		if (typeName.equals("integer"))
			return "int";
		if (typeName.equals("int2"))
			return "short";
		if (typeName.equals("int4"))
			return "int";
		if (typeName.equals("int8"))
			return "long";
		if (typeName.equals("long"))
			return "long";
		if (typeName.equals("bigint"))
			return "long";
		if (typeName.equals("smallint"))
			return "short";
		if (typeName.equals("short"))
			return "short";
		if (typeName.equals("double"))
			return "double";
		if (typeName.equals("float"))
			return "double";
		if (typeName.equals("real"))
			return "real";
		if (typeName.equals("float8"))
			return "double";
		if (typeName.equals("float4"))
			return "double";
		if (typeName.equals("float"))
			return "double";
		if (typeName.equals("number"))
			return "numeric";
		if (typeName.equals("numeric"))
			return "numeric";
		if (typeName.equals("decimal"))
			return "numeric";
		if (typeName.equals("boolean"))
			return "boolean";
		if (typeName.equals("bool"))
			return "boolean";
		if (typeName.equals("char"))
			return "char";
		if (typeName.equals("bpchar"))
			return "char";
		if (typeName.equals("varchar2"))
			return "string";
		if (typeName.equals("varchar"))
			return "string";
		if (typeName.equals("text"))
			return "string";
		if (typeName.equals("citext"))
			return "string";
		if (typeName.equals("character"))
			return "string";
		if (typeName.equals("string"))
			return "string";
		if (typeName.equals("timestamp"))
			return "timestamp";
		if (typeName.equals("timestamptz"))
			return "timestamp";
		if (typeName.equals("date"))
			return "date";
		if (typeName.equals("unknown"))
			return "unknown";
		return typeName;
	}

	public static boolean isNumericType(String typeName) {
		return defaultIsNumericType(typeName);
	}

	public static boolean defaultIsNumericType(String typeName) {
		typeName = convertTypeIntoDefaultSynonim(typeName);
		if (typeName.equals("int"))
			return true;
		if (typeName.equals("short"))
			return true;
		if (typeName.equals("long"))
			return true;
		if (typeName.equals("double"))
			return true;
		if (typeName.equals("real"))
			return true;
		if (typeName.equals("numeric"))
			return true;

		return false;
	}

	private static boolean match(String var, String value) {
		if (var != null)
			return var.equalsIgnoreCase(value);
		else
			return value == null;
	}

	private static boolean matchAny(String var, String... values) {
		for (String v : values) {
			if (match(var, v))
				return true;
		}
		return false;
	}

	private static boolean match(String a, String b, String va, String vb) {
		boolean r = match(a, va) && match(b, vb);
		if (r)
			return r;
		r = match(a, vb) && match(b, va);
		return r;
	}

	public boolean isInsertOrUpdateCommand(String sql) {
		String s = sql.toLowerCase().trim();
		if (s.startsWith("update"))
			return true;
		if (s.startsWith("insert"))
			return true;

		return false;
	}

	public String inferTableName(String sql) {
		sql = sql.trim();
		if (Util.startsWithIgnoreCase(sql, "update"))
			return inferTableNameFromUpdate(sql);
		if (Util.startsWithIgnoreCase(sql, "insert"))
			return inferTableNameFromInsert(sql);

		throw new RuntimeException("Não foi possível inferir o nome da tabela em " + sql);
	}

	private String inferTableNameFromUpdate(String sql) {
		String[] ss = sql.split("^update\\s|\\sset\\s");
		if (ss.length < 2)
			throw new RuntimeException("Não foi possível inferir o nome da tabela");

		String r = ss[1].trim();
		return r;
	}

	private String inferTableNameFromInsert(String sql) {
		String[] ss = sql.split("^insert\\sinto\\s|\\s");
		if (ss.length < 2)
			throw new RuntimeException("Não foi possível inferir o nome da tabela");

		String r = ss[1].trim();
		return r;
	}


}
