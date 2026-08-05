package core.parsing.string;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import core.parsing.string.StringBuilder3.Transaction;

public class StringBuilder2 implements DBSStringBuilder {

	private ChangeHistory history = new ChangeHistory();

	public ChangeHistory history() {
		return history;
	}

	void setHistory(ChangeHistory h) {
		this.history = h;
	}

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
		history.add(s);
	}

	public void appendIfNotEmpty(final String separator, final String s2) {
		if (length() > 0)
			append(separator);
		if (s2 != null)
			append(s2);
	}

	public void setLength(int length) {
		history.remove(this.toString());
		original.setLength(0);
		lowerCase.setLength(0);
	}

	public void insert(int offset, String s) {
		original.insert(offset, s);
		lowerCase.insert(offset, s.toLowerCase());
		history.add(s);
	}

	public void trimToSize() {
		original.trimToSize();
		lowerCase.trimToSize();
	}

	public StringBuilder getOriginalBuilder() {
		return original;
	}

	public StringBuilder2 copy() {
		StringBuilder2 cloned = new StringBuilder2();
		cloned.original = new StringBuilder(this.original);
		cloned.lowerCase = new StringBuilder(this.lowerCase);
		return cloned;
	}

	public boolean contains(String s) {
		return contains(s, false);
	}

	public boolean contains(String s, boolean ignoreCase) {
		if (ignoreCase)
			return lowerCase.indexOf(s.toLowerCase()) != -1;
		else
			return original.indexOf(s) != -1;
	}

	public String[] find(String regex) {
		Pattern pattern = Pattern.compile(regex, 0);
		return find(pattern);
	}

	public String[] find(Pattern pattern) {
		String content = original.toString();
		Matcher matcher = pattern.matcher(content);

		java.util.List<String> matches = new java.util.ArrayList<>();
		while (matcher.find()) {
			matches.add(matcher.group());
		}

		return matches.toArray(new String[0]);
	}

	public void replace(String from, String to) {
		replace(from, to, false, false);
	}

	public void replace(String from, String to, boolean ignoreCase, boolean onlyFirst) {
		int idx = 0;
		if (to == null)
			throw new RuntimeException("argumento nulo");

		String targetLower = from.toLowerCase();

		StringBuilder stream = original;
		if (ignoreCase)
			stream = lowerCase;

		while ((idx = stream.indexOf(targetLower, idx)) != -1) {
			original.replace(idx, idx + from.length(), to);
			lowerCase.replace(idx, idx + from.length(), to.toLowerCase());
			history.replace(from, to);
			if (onlyFirst)
				break;
		}
	}

	public void replace(String regex, Function<Matcher, String> replacement) {
		Pattern pattern = Pattern.compile(regex, 0);
		replace(pattern, replacement);
	}

//	@Deprecated
//	public void replace(String regex, int flags, Function<Matcher, String> replacement) {
//	    if (regex == null || regex.isEmpty() || replacement == null) {
//	        return;
//	    }
//
//	    Pattern pattern = Pattern.compile(regex, flags);
//	    replace(pattern, replacement);
//	}

	public void replace(Pattern pattern, Function<Matcher, String> replacement) {
		String content = original.toString();
		Matcher matcher = pattern.matcher(content);

		StringBuilder resultOriginal = new StringBuilder();
		StringBuilder resultLowerCase = new StringBuilder();

		int lastEnd = 0;

		while (matcher.find()) {
			// Adiciona o trecho antes da correspondência
			String beforeMatch = content.substring(lastEnd, matcher.start());
			resultOriginal.append(beforeMatch);
			resultLowerCase.append(beforeMatch.toLowerCase());

			// Aplica a função de substituição
			String fromText = matcher.group();
			String toText = replacement.apply(matcher);
			if (toText == null) {
				toText = "null";
			}
			this.history.replace(fromText, toText);

			resultOriginal.append(toText);
			resultLowerCase.append(toText.toLowerCase());

			lastEnd = matcher.end();
		}

		// Adiciona o restante do texto
		if (lastEnd < content.length()) {
			String remaining = content.substring(lastEnd);
			resultOriginal.append(remaining);
			resultLowerCase.append(remaining.toLowerCase());
		}

		// Substitui os StringBuilders internos
		original = resultOriginal;
		lowerCase = resultLowerCase;
	}

	@Override
	public Transaction newTransaction(Predicate<String> newValueCriteria) {
		return null;
	}

}