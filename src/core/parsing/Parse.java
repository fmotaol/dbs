package core.parsing;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse {

	public static final String REGEX_ID = "[A-Za-z_][A-Za-z0-9_]*";
	public static final String REGEX_INT = "-?[0-9]+";

	public static boolean isIdentifier(String s) {
		s = s.replaceAll(REGEX_ID, "");
		return s.length() == 0;
	}

	public static boolean isSQLIdentifier(String s) {
		return isIdentifier(s);
	}


	public static boolean match(String text, String regex) {
		Pattern p = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(text);
		return matcher.find();
	}
	
	public static boolean isInteger(String s) {
		Pattern p = Pattern.compile("^" + REGEX_INT + "$", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(s);
		return matcher.find();
	}
	
	public static String[] arguments(String argList) {
		String regex = "[^\\s\"']+|\"([^\"]*)\"|'([^']*)'";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(argList);
		ArrayList<String> list = new ArrayList<String>(); 
		while (m.find()) {
		    if (m.group(1) != null) {
		        // Add double-quoted string without the quotes
		        list.add(m.group(1));
		    } else if (m.group(2) != null) {
		        // Add single-quoted string without the quotes
		        list.add(m.group(2));
		    } else {
		        // Add unquoted word
		        list.add(m.group());
		    }
		} 	
		String[] r = new String[list.size()];
		r = (String[]) list.toArray(r);
		return r;
	}
	
}
