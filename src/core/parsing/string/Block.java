package core.parsing.string;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Block {

	public StringBuilder getOriginalBuilder();
	
	
	public boolean contains(String s);
	
	public boolean contains(String s, boolean ignoreCase);
	
	default public String[] find(String regex) {
		Pattern pattern = Pattern.compile(regex, 0);
		return find(pattern);
	}
	
	public String[] find(Pattern pattern);
	
	public ChangeHistory history();
	
	
	
	public void appendIfNotEmpty(String separator, String s);

	
	
	public void setLength(int length);

	public void insert(int offset, String s);

	public void trimToSize();
	
	default public void replace(String from, String to) {
		replace(from, to, false, false);
	}
	
	public void replace(String from, String to, boolean ignoreCase, boolean onlyFirst);
	
	default public void replace(String regex, Function<Matcher, String> replacement) {
		replace(Pattern.compile(regex), replacement);
	}

	public void replace(Pattern pattern, Function<Matcher, String> replacement);
	
	
	public Transaction newTransaction(Predicate<String> newValueCriteria);
	
	//public void commit();
	
	//public void rollback();
	
	
	
	public String toString();

	public int length();

	public void append(String s);
	

	
}
