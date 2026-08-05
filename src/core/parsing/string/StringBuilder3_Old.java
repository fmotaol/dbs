package core.parsing.string;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.Util;

public class StringBuilder3_Old {
	
	private StringBuilder2 content;

	public StringBuilder3_Old() {
		content = new StringBuilder2();
	}

	public StringBuilder3_Old(String s) {
		content = new StringBuilder2(s);
	}

	@Override
	public String toString() {
		return content.toString();
	}

	public int length() {
		return content.length();
	}

	private ChangeHistory history = new ChangeHistory();

	private Transaction activeTransaction;

	public void append(String s) {
		if (activeTransaction != null) {
			activeTransaction.append(s);
			return;
		}

		unmanagedAppend(s);
		history.add(s);
	}

	private void unmanagedAppend(String s) {
		original.append(s);
		lowerCase.append(s.toLowerCase());
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
		if (activeTransaction != null) {
			activeTransaction.insert(offset, s);
			return;
		}
		
		unmanagedInsert(offset, s);
		history.add(s);
	}

	private void unmanagedInsert(int offset, String s) {
		original.insert(offset, s);
		lowerCase.insert(offset, s.toLowerCase());
	}

	public void trimToSize() {
		original.trimToSize();
		lowerCase.trimToSize();
	}

	public boolean containsIgnoreCase(String s) {
		s = s.toLowerCase();
		boolean r = contains(lowerCase, s);
		return r;
	}

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

	public StringBuilder getOriginalBuilder() {
		return original;
	}

	public StringBuilder3_Old copy() {
		StringBuilder3_Old cloned = new StringBuilder3_Old();
		cloned.original = new StringBuilder(this.original);
		cloned.lowerCase = new StringBuilder(this.lowerCase);
		return cloned;
	}

	public boolean contains(String s) {
		return contains(original, s);
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

	private void unmanagedReplaceString(String from, String to, boolean ignoreCase, boolean onlyFirst) {
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

	private void replaceString(String from, String to, boolean ignoreCase, boolean onlyFirst) {
		if (activeTransaction != null) {
			to = activeTransaction.replace(from, to, ignoreCase, onlyFirst);
			return;s
		}

		unmanagedReplaceString(from, to, ignoreCase, onlyFirst);
		history.replace(from, to);
	}

	private void unmanagedReplace(String from, String to) {
		String s = original.toString();
		s = s.replace(from, to);
		original = new StringBuilder(s);
		lowerCase = new StringBuilder(s.toLowerCase());
	}

	public void replace(String from, String to) {
		if (activeTransaction != null) {
			to = activeTransaction.replace(from, to);
			return;
		}
		
		unmanagedReplace(from, to);
		history.replace(from, to);
	}

	public void replace(String regex, Function<Matcher, String> replacement) {
		Pattern pattern = Pattern.compile(regex, 0);
		replace(pattern, replacement);
	}

	public void replace(Pattern pattern, Function<Matcher, String> replacement) {
		String content = original.toString();
		Matcher matcher = pattern.matcher(content);

		StringBuilder resultOriginal = new StringBuilder();

		int lastEnd = 0;

		while (matcher.find()) {
			// Adiciona o trecho antes da correspondência
			String beforeMatch = content.substring(lastEnd, matcher.start());
			resultOriginal.append(beforeMatch);

			// Aplica a função de substituição
			String fromText = matcher.group();
			String toText = replacement.apply(matcher);
			if (toText == null)
				throw new RuntimeException("Texto nulo");

			if (activeTransaction != null) {
				activeTransaction.replaceString(fromText, toText);
			} else {			
				resultOriginal.append(toText);			
				history.replace(fromText, toText);
			}

			lastEnd = matcher.end();
		}

		// Adiciona o restante do texto
		if (lastEnd < content.length()) {
			String remaining = content.substring(lastEnd);
			resultOriginal.append(remaining);
		}

		// Substitui os StringBuilders internos
		original = resultOriginal;
		lowerCase = new StringBuilder(original.toString().toLowerCase());
	}

	public class Transaction {
		
		public enum State {ACTIVE, COMMITED, CANCELLED};
		
		private State state = State.ACTIVE;
		
		private String originalString;
		
		private Map<String, String> fromMap = new HashMap<>();
		private Map<String, String> toMap = new HashMap<>();

		public void commit() {
			for (String k : toMap.keySet()) {
				String from = fromMap.get(k);
				String to = toMap.get(k);
				if (from != null && to != null) { //replace
					
					StringBuilder3_Old.this.unmanagedReplaceString(k, to);
					StringBuilder3_Old.this.history.replacements.put(from, to);
					
				} else if (to != null) { //insert ou append
					
					StringBuilder3_Old.this.unmanagedReplaceString(k, to);
					StringBuilder3_Old.this.history.additions.add(to);
					
				} else if (from != null) { //remove
					
					StringBuilder3_Old.this.history.exclusions.add(from);
					
				}
			}
			
			state = State.COMMITED;
			StringBuilder3_Old.this.activeTransaction = null;
		}

		public void rollback() {
			StringBuilder3_Old.this.original = new StringBuilder(originalString);
			StringBuilder3_Old.this.lowerCase = new StringBuilder(originalString.toLowerCase());
			state = State.CANCELLED;
		}
		
		public State getState() {
			return state;
		}

		public String scrambleKey(String fromText, String toText) {
			String k = generateScrambleKey();
			if (fromText != null)
				fromMap.put(k, fromText);
			if (toText != null)
				toMap.put(k, toText);
			return k;
		}

		private String generateScrambleKey() {
			String r = null;
			int i = 1;
			do {
				String rand = (Math.random() + "").replace("0.", "");
				r = "##_SCRAMBLE_" + rand + "_SCRAMBLE_##";
				i++;
			} while (fromMap.get(r) != null && toMap.get(r) != null && i < 500);

			if (i >= 500)
				throw new RuntimeException("Erro interno. Repetição de chave SCRAMBLE");

			return r;
		}

	}

	public ChangeHistory history() {
		return history;
	}

}