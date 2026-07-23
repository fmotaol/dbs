package util;

public class StringBuilder2 {

	public StringBuilder2() {
		original = new StringBuilder();
		lowerCase = new StringBuilder();
	}

	public StringBuilder2(String s) {
		original = new StringBuilder(s);
		lowerCase = new StringBuilder(s.toLowerCase());
	}

	private StringBuilder original;

	private StringBuilder lowerCase;

	@Override
	public String toString() {
		return original.toString();
	}

	public int length() {
		return original.length();
	}

	public void append(String s) {
		original.append(s);
		lowerCase.append(s.toLowerCase());
	}

	public void setLength(int length) {
		original.setLength(0);
		lowerCase.setLength(0);
	}

	public void insert(int offset, String s) {
		original.insert(offset, s);
		lowerCase.insert(offset, s.toLowerCase());
	}

	public void trimToSize() {
		original.trimToSize();
		lowerCase.trimToSize();
	}

	// private HashMap<String, Boolean> containsCheckCache = new HashMap<String,
	// Boolean>();

	public boolean containsIgnoreCase(String s) {

		// Boolean r = containsCheckCache.get(s);
		// if (r != null) {
		// return r;
		// } else {
		s = s.toLowerCase();
		boolean r = contains(lowerCase, s);
		// containsCheckCache.put(s, r);
		// }

		return r;
	}

	// private static HashMap<String, String> temp = new HashMap<String,
	// String>();

	// private static int counter = 0;

	public static boolean contains(StringBuilder sb, String s) {
		// Date t = new Date();
		// TODO revisar performance
		boolean r = sb.indexOf(s) >= 0;
		// String content = sb.toString();
		// String inverseKey = temp.get(content);
		// if (inverseKey == null) {
		// inverseKey = counter + "";
		// counter++;
		// temp.put(content, inverseKey);
		// }
		// boolean r = sb.toString().contains(s);
		// Util.logTime("contains " + inverseKey + " --> " + s, t);
		return r;
	}

	// public StringBuilder getLowerCaseBuilder() {
	// return lowerCase;
	// }

	public StringBuilder getOriginalBuilder() {
		return original;
	}

	public void replaceIgnoreCase(final String from, final String to) {
		replaceIgnoreCase(from, to, false);
	}

	public void replaceFirstIgnoreCase(String from, String to) {
		replaceIgnoreCase(from, to, true);
	}

	private void replaceIgnoreCase(final String target, String replacement, final boolean onlyFirst) {
		int idx = 0;
		if (replacement == null)
			throw new RuntimeException("argumento nulo");

		String targetLower = target.toLowerCase();
		while ((idx = lowerCase.indexOf(targetLower, idx)) != -1) {
			original.replace(idx, idx + target.length(), replacement);
			lowerCase.replace(idx, idx + target.length(), replacement);
			if (onlyFirst)
				break;
		}
	}

	public boolean contains(String s) {
		return contains(original, s);
	}

}
