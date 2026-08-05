package core.parsing.string;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface DBSStringBuilder {

	public StringBuilder getOriginalBuilder();
	
	
	public boolean contains(String s);
	
	public boolean contains(String s, boolean ignoreCase);
	
	public String[] find(String regex);
	
	public String[] find(Pattern pattern);
	
	public ChangeHistory history();
	
	
	
	public void appendIfNotEmpty(String separator, String s);

	
	
	public void setLength(int length);

	public void insert(int offset, String s);

	public void trimToSize();
	
	public void replace(String from, String to);
	
	public void replace(String from, String to, boolean ignoreCase, boolean onlyFirst);
	
	public void replace(String regex, Function<Matcher, String> replacement);

	public void replace(Pattern pattern, Function<Matcher, String> replacement);
	
	
	
	public String toString();

	public int length();

	public void append(String s);
	

}
