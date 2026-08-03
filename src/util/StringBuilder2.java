package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringBuilder2 {

	private Map<String, String> replacements = new HashMap<>(); 

	private ArrayList<String> additions = new ArrayList<>(); 

	private ArrayList<String> exclusions = new ArrayList<>(); 

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
		additions.add(s);
	}

	public void setLength(int length) {
		original.setLength(0);
		lowerCase.setLength(0);
		exclusions.add(this.toString());
	}

	public void insert(int offset, String s) {
		original.insert(offset, s);
		lowerCase.insert(offset, s.toLowerCase());
		additions.add(s);
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
			replacements.put(target, replacement);
			if (onlyFirst)
				break;
		}
	}

	public boolean contains(String s) {
		return contains(original, s);
	}

	public String[] find(String regex) {
		Pattern pattern = Pattern.compile(regex, 0);
		return find(pattern);
	}

	@Deprecated
	public String[] find(String regex, int flags) {
		if (regex == null || regex.isEmpty()) {
			return new String[0];
		}

		Pattern pattern = Pattern.compile(regex, flags);
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

	@Deprecated
	public void replace(String regex, int flags, Function<Matcher, String> replacement) {
	    if (regex == null || regex.isEmpty() || replacement == null) {
	        return;
	    }

	    Pattern pattern = Pattern.compile(regex, flags);
	    replace(pattern, replacement);
	}
	
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
			this.replacements.put(fromText, toText);
	        
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
	
	public void replace(String regex, Function<Matcher, String> replacement) {
		Pattern pattern = Pattern.compile(regex, 0);
	    replace(pattern, replacement);
	}
	
	public Map<String, String> getReplacements() {
	    return Collections.unmodifiableMap(replacements);
	}

	public List<String> getAdditions() {
	    return Collections.unmodifiableList(additions);
	}

	public List<String> getExclusions() {
	    return Collections.unmodifiableList(exclusions);
	}

}