package util;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class Util {

	public static String concat(String[] array, String separator) {
		StringBuilder r = new StringBuilder();
		appendWithSeparator(r, array, separator);
		String s = r.toString();
		r.setLength(0);
		r.trimToSize();
		return s;
	}

	public static String[] splitByComma(String sentence, boolean trimElements) {
		String ss[] = sentence.split(",");
		if (trimElements)
			for (int i = 0; i < ss.length; i++)
				ss[i] = ss[i].trim();
		return ss;
	}

	public static void appendWithSeparator(StringBuilder sb, String[] array, String separator) {
		for (String e : array) {
			if (e.isEmpty())
				continue;
			if (sb.length() > 0)
				sb.append(separator);
			sb.append(e);
		}
	}

	public static String[] concat(String[] ar, String... es) {
		String[] r = Arrays.copyOf(ar, ar.length + es.length);
		for (int i = ar.length; i < r.length; i++) {
			r[i] = es[i - ar.length];
		}
		return r;
	}

	public static String repeat(String s, int times) {
		StringBuilder r = new StringBuilder();
		for (int i = 1; i <= times; i++) {
			r.append(s);
		}
		s = r.toString();
		r.setLength(0);
		r.trimToSize();
		return s;
	}

	public static String elapsedTimeText(Date start) {
		Date now = new Date();
		return elapsedTimeText(start, now);
	}

	private static final DecimalFormat defaultDecimalFormat = new DecimalFormat("#.###");

	public static String elapsedTimeText(double elapsedTimeMiliSecs) {
		if (elapsedTimeMiliSecs < 0.001)
			return defaultDecimalFormat.format(elapsedTimeMiliSecs * 1000000.0) + "ns";

		if (elapsedTimeMiliSecs < 1.0)
			return defaultDecimalFormat.format(elapsedTimeMiliSecs * 1000.0) + "us";

		double ms = elapsedTimeMiliSecs;
		if (ms < 1000)
			return defaultDecimalFormat.format(ms) + "ms";

		double s = ms / 1000.0;
		if (s < 5)
			return String.format("%.3fs", s);
		if (s < 10)
			return String.format("%.2fs", s);
		if (s < 60)
			return String.format("%.1fs", s);

		double min = s / 60.0;

		if (min < 60) {
			double fmin = Math.floor(min);
			double seconds = s - fmin * 60;
			if (seconds < 1)
				return String.format("%.0fmin", min);

			return String.format("%.0fmin ", fmin) + String.format("%.0fs", seconds);
		}

		double h = min / 60.0;
		double fh = Math.floor(h);
		double pmin = min - fh * 60;
		if (pmin < 1)
			return String.format("%.0fh", fh);

		return String.format("%.0fh ", fh) + String.format("%.0fmin", pmin);
	}

	public static String elapsedTimeText(Date start, Date end) {
		long t = elapsedTime(start, end);
		return elapsedTimeText(t);
	}

	private static long elapsedTime(Date start, Date end) {
		return end.getTime() - start.getTime();
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
		return true;
	}

	public static boolean isNumber(String s) {
		try {
			Float.parseFloat(s);
		} catch (NumberFormatException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
		return true;
	}


	public static boolean containsIgnoreCase(StringBuilder sb, String part) {
		return sb.toString().toLowerCase().contains(part.toLowerCase());
	}

	public static boolean containsIgnoreCase(String s, String part) {
		return s.toLowerCase().contains(part.toLowerCase());
	}

	public static void replaceFirstIgnoreCase(final StringBuilder source, final String target, String replacement) {
		replaceIgnoreCase(source, target, replacement, true);
	}

	public static void replaceIgnoreCase(final StringBuilder source, final String target, final String replacement) {
		replaceIgnoreCase(source, target, replacement, false);
	}

	private static void replaceIgnoreCase(final StringBuilder source, final String target, final String replacement,
			boolean onlyFirst) {
		StringBuilder sbSourceLower = new StringBuilder(source.toString().toLowerCase());
		String searchString = target.toLowerCase();

		int idx = 0;
		while ((idx = sbSourceLower.indexOf(searchString, idx)) != -1) {
			source.replace(idx, idx + searchString.length(), replacement);
			if (onlyFirst)
				break;
			else {
				sbSourceLower.replace(idx, idx + searchString.length(), replacement);
				idx += replacement.length();
			}
		}
		sbSourceLower.setLength(0);
		sbSourceLower.trimToSize();
		sbSourceLower = null;
	}

	public static String replaceIgnoreCase(final String source, final String target, final String replacement) {
		StringBuilder r = new StringBuilder(source);
		replaceIgnoreCase(r, target, replacement);
		String s = r.toString();
		r.setLength(0);
		r.trimToSize();
		return s;
	}

	public static boolean startsWithIgnoreCase(final String s, final String start) {
		return s.toLowerCase().startsWith(start.toLowerCase());
	}

	public static String newUnusedSymbolFor(String text) {
		String r;
		do {
			r = "$_" + Math.random() + "_$";
		} while (text.contains(r));
		return r;
	}

	public static boolean contains(final String[] list, final String string) {
		for (String s : list)
			if (s.equals(string))
				return true;
		return false;
	}

	public static boolean containsIgnoreCase(final String[] list, final String string) {
		for (String s : list)
			if (s.equalsIgnoreCase(string))
				return true;
		return false;
	}

	public static void concat(final StringBuilder sb, final String... e) {
		for (int i = 0; i < e.length; i++) {
			sb.append(e[i]);
		}
	}

	public static String concat(final String... e) {
		StringBuilder sb = new StringBuilder();
		concat(sb, e);
		String s = sb.toString();
		sb.setLength(0);
		sb.trimToSize();
		return s;
	}

	@Deprecated
	public static String concatOnlyValues(final String v1, final String separator, final String v2) {
		if ((v1 == null) || v1.isEmpty())
			return v2;
		if ((v2 == null) || v2.isEmpty())
			return v1;
		return concat(v1, separator, v2);
	}

	public static void appendIfNotEmpty(final StringBuilder sb, final String separator, final String s2) {
		if (sb.length() > 0)
			sb.append(separator);
		if (s2 != null)
			sb.append(s2);
	}

	@Deprecated
	public static void appendIfNotEmpty(final StringBuilder2 sb, final String separator, final String s2) {
		if (sb.length() > 0)
			sb.append(separator);
		if (s2 != null)
			sb.append(s2);
	}

	public static void append(StringBuilder sb, String... ss) {
		for (String s : ss)
			sb.append(s);
	}

	public static boolean contains(StringBuilder sql, String string) {
		return sql.toString().contains(string);
	}

	public static void replaceFirst(StringBuilder source, String target, String replacement) {
		replace(source, target, replacement, true);
	}

	public static void replace(StringBuilder source, String target, String replacement, boolean onlyFirst) {
		int idx = 0;
		while ((idx = source.indexOf(target, idx)) != -1) {
			source.replace(idx, idx + target.length(), replacement);
			if (onlyFirst)
				break;
			else {
				idx += replacement.length();
			}
		}
	}

	public static void replaceRegex(StringBuilder sql, String regex, String replacement) {
		String s = sql.toString();
		s = s.replaceAll(regex, replacement);
		sql.setLength(0);
		sql.append(s);
	}

	public static boolean containsRegex(StringBuilder sql, String regex) {
		String s = sql.toString();
		String r = s.replaceAll(regex, "");
		return !s.equals(r);
	}

	public static final String regexIdentifier = "[a-zA-Z_][a-zA-Z\\d_]*";

	private static HashMap<String, Long> accumElapsedTimes = new HashMap<String, Long>();

	private static HashMap<String, Long> executedOperations = new HashMap<String, Long>();

	public static void logTime(String action, Date start) {
		logTime(action, start, true);
	}

	static Date startMonitoring = null;

	static long accumOverhead = 0;

	public synchronized static void logTime(String operation, Date start, boolean show) {
//		long overhead
		Date d = new Date();

		Long act = accumElapsedTimes.get(operation);
		if (act == null)
			act = (long) 0;

		Long acop = executedOperations.get(operation);
		if (acop == null)
			acop = (long) 0;

		long elapsed = elapsedTime(start, d);
		act += elapsed;
		acop += 1;

		accumElapsedTimes.put(operation, act);
		executedOperations.put(operation, acop);

		if (show) {
			if (startMonitoring == null)
				startMonitoring = new Date();

			long totaltime = elapsedTime(startMonitoring, d);
			long usefulTime = totaltime - accumOverhead;

			System.out.println(Util.concat(operation, " No ", acop + ", elapsed=", elapsedTimeText(elapsed), " acum=",
					elapsedTimeText(act), "/", elapsedTimeText(usefulTime),
					String.format(" (%.1f", 100 * act / (usefulTime + 0.0)), "%)", " average=",
					elapsedTimeText(act / (acop + 0.0)), "/op"));
		}
		Date f = new Date();
		long overhead = elapsedTime(d, f);
		accumOverhead += overhead;
	}

	public static final SimpleDateFormat baseTimestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.");

	public static String timestampFormat(Timestamp time) {
		int microFraction = time.getNanos() / 1000;

		StringBuilder sb = new StringBuilder(baseTimestampFormat.format(time));
		String tail = String.valueOf(microFraction);
		for (int i = 0; i < 6 - tail.length(); i++) {
			sb.append('0');
		}
		sb.append(tail);
		String s = sb.toString();
		sb.setLength(0);
		sb.trimToSize();
		return s;
	}

	public static String[] trim(String[] s) {
		String[] r = new String[s.length];
		for (int i = 0; i < s.length; i++) {
			if (s[i] != null)
				r[i] = s[i].trim();
			else
				r[i] = null;
		}
		return r;
	}

	public static String abrev(String msg, int size) {
		if (msg.length() > size)
			return msg.substring(0, size) + "...";
		else
			return msg;
	}

	public static <T> T ifNull(T a, T b) {
		if (a != null)
			return a;
		else
			return b;
	}

	public static <T> T ifThen(boolean condition, T a, T b) {
		if (condition)
			return a;
		else
			return b;
	}

	public static <T> T ifThen(boolean condition, T a) {
		return ifThen(condition, a, null);
	}

	public static void confirm(boolean condition, String msg) {
		if (!condition)
			throw new RuntimeException(msg);
	}

	public static void confirm(boolean condition) {
		confirm(condition, "Condi��o violada");
	}

	public static Boolean compare(String operand1, String operator, String operand2) {
		if ((operand1 == null) || (operator == null) || (operand2 == null))
			return null;

		if ("=".equals(operator)) {
			return (operand1.equals(operand2));
		}

		if ("<>".equals(operator)) {
			return (!operand1.equals(operand2));
		}

		throw new RuntimeException("Operadores <, <=, >= e > n�o s�o aplic�veis aos operandos " + operand1 + " e " + operand2);
	}

	public static boolean compare(float operand1, String operator, float operand2) {

		if (operator.equals("=")) {
			return (operand1 == operand2);
		}

		if (operator.equals("<>")) {
			return (operand1 != operand2);
		}

		if (operator.equals("<")) {
			return (operand1 < operand2);
		}

		if (operator.equals("<=")) {
			return (operand1 <= operand2);
		}

		if (operator.equals(">=")) {
			return (operand1 >= operand2);
		}

		if (operator.equals(">")) {
			return (operand1 > operand2);
		}
		throw new RuntimeException("Operador n�o suportado: " + operator);
	}

	public static String left(String s, int chars) {
		s = s.substring(0, chars);
		return s;
	}

	public static String right(String s, int chars) {
		int e = s.length() - chars;
		s = s.substring(e, s.length());
		return s;
	}

	public static String removeLeft(String s, int chars) {
		s = s.substring(chars);
		return s;
	}

	public static String removeRight(String s, int chars) {
		int e = s.length() - chars;
		s = s.substring(0, e);
		return s;
	}

	public static <T> void copyRight(T[] source, T[] dest, int offset) {
		if (source.length != dest.length + offset)
			throw new RuntimeException("Tamanhos de array incompat�veis");

		for (int i = 0; i < dest.length; i++) {
			dest[i] = source[i + offset];
		}
	}

	public static String quoted(String s) {
		if (s.contains("\""))
			s = s.replace("\"", "\"\"");

		s = "\"" + s + "\"";
		return s;
	}

	public static boolean isAny(String element, String... list) {
		for (String e : list) {
			if (e == null)
				continue;
			if (e.equals(element))
				return true;
		}
		return false;
	}

	public static boolean isAnyIgnoreCase(String element, String... list) {
		for (String e : list) {
			if (e == null)
				continue;
			if (e.equalsIgnoreCase(element))
				return true;
		}
		return false;
	}

	public static void throwAsRuntimeException(Throwable e) {
		if (e == null)
			return;
		if (e instanceof RuntimeException)
			throw (RuntimeException) e;
		throw new RuntimeException(e);
	}

	public static boolean nullOrEmpty(String command) {
		if (command == null)
			return true;

		if ("".equals(command.trim()))
			return true;
		
		return false;
	}


}
