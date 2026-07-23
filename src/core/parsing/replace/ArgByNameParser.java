package core.parsing.replace;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.StringBuilder2;

public class ArgByNameParser {

	// private static final String contentRegex = "(?<content>.*)";

	private static final String regexContent = "(?<content>[^\\]]*)";

	public static final String regexArgByName = Pattern.quote("@arg[") + regexContent + "\\]";

	private static Pattern mainPattern = Pattern.compile(regexArgByName, Pattern.DOTALL
			| Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	public static String[] listArgNames(final StringBuilder2 sql) {

		ArrayList<String> a = new ArrayList<String>(); 
		
		Matcher matcher = mainPattern.matcher(sql.toString());

		while (matcher.find()) {

			String group = matcher.group(0);

			if (group.toLowerCase().startsWith("@arg[")) {

				String name = matcher.group("content");
				if (a.contains(name))
					continue;
				a.add(name);
			}
		}
		
		String[] r = new String[a.size()];
		r = a.toArray(r);
		return r;
		

	}
}
