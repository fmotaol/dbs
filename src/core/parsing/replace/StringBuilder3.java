package core.parsing.replace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.Util;

public class StringBuilder3 {

	public StringBuilder3() {
		original = new StringBuilder();
		lowerCase = new StringBuilder();
	}

	public StringBuilder3(String s) {
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

	public class ChangeHistory {

		private Map<String, String> replacements = new HashMap<>();

		private ArrayList<String> additions = new ArrayList<>();

		private ArrayList<String> exclusions = new ArrayList<>();

		public Map<String, String> getReplacements() {
			return Collections.unmodifiableMap(replacements);
		}

		public List<String> getAdditions() {
			return Collections.unmodifiableList(additions);
		}

		public List<String> getExclusions() {
			return Collections.unmodifiableList(exclusions);
		}

		private void add(String s) {
			additions.add(s);
		}

		private void remove(String s) {
			exclusions.add(s);
		}

		private void replace(String from, String to) {
			replacements.put(from, to);
		}

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
		original.setLength(0);
		lowerCase.setLength(0);
		history.remove(this.toString());
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

	public StringBuilder2 clone() {
		StringBuilder2 cloned = new StringBuilder2();
		cloned.original = new StringBuilder(this.original);
		cloned.lowerCase = new StringBuilder(this.lowerCase);
		return cloned;
	}

	public void replaceIgnoreCase(final String from, final String to) {
		replaceIgnoreCase(from, to, false);
	}

	public void replaceFirst(String from, String to) {
		Util.replaceFirst(original, from, to);
		lowerCase = new StringBuilder(original.toString().toLowerCase());
		history.replace(from, to);
	}

	private void unmanagedReplaceString(String from, String to, boolean ignoreCase, boolean onlyFirst) {
		int idx = 0;
		if (to == null)
			throw new RuntimeException("argumento nulo");

		String targetLower = from.toLowerCase();
		while ((idx = lowerCase.indexOf(targetLower, idx)) != -1) {
			original.replace(idx, idx + from.length(), to);
			lowerCase.replace(idx, idx + from.length(), to);

			if (onlyFirst)
				break;
		}
		
	}

	private void replaceString(String from, String to, boolean ignoreCase, boolean onlyFirst) {
		if (activeTransaction != null) {
			to = activeTransaction.replaceString(from, to, ignoreCase, onlyFirst);
			return;
		}

		history.replace(from, to); //Só terá uma entrada mesmo
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

	private void unmanagedReplaceString(String from, String to) {
		String s = original.toString();
		s = s.replace(from, to);
		original = new StringBuilder(s);
		lowerCase = new StringBuilder(s.toLowerCase());
	}

	public void replaceString(String from, String to) {
		if (activeTransaction != null) {
			to = activeTransaction.replaceString(from, to);
			return;
		}
		
		unmanagedReplaceString(from, to);
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

//			if (activeTransaction != null)
//				toText = activeTransaction.scramble(fromText, toText);
			
			resultOriginal.append(toText);

			history.replace(fromText, toText);

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

		private Map<String, String> fromMap = new HashMap<>();
		private Map<String, String> toMap = new HashMap<>();

		public void commit() {
			for (String k : toMap.keySet()) {
				String from = fromMap.get(k);
				String to = toMap.get(k);
				if (from != null && to != null) {
					StringBuilder3.this.unmanagedReplaceString(from, to);
					StringBuilder3.this.history.replacements.put(from, to);
				} else if (from != null && to != null) {
					StringBuilder3.this.unmanagedAppend(from, to);
					StringBuilder3.this.history.replacements.put(from, to);
				}
			}
			throw new RuntimeException("ainda não implementado");
		}

		public String scrambleKey(String fromText, String toText) {
			String k = generateScrambleKey();
			if (fromText != null)
				fromMap.put(k, fromText);
			if (toText != null)
				toMap.put(k, toText);
			return k;
		}

		public void rollback() {
			throw new RuntimeException("Rollback não suportado");
		}

		private String generateScrambleKey() {
			String r = null;
			int i = 1;
			do {
				String rand = (Math.random() + "").replace("0.", "");
				r = "##_SCRAMBLE_" + rand + "_SCRAMBLE_##";
				i++;
			} while (scrambleMap.get(r) != null && i < 500);

			if (i >= 500)
				throw new RuntimeException("Erro interno. Repetição de chave SCRAMBLE");

			return r;
		}

	}

	public ChangeHistory history() {
		return history;
	}

}