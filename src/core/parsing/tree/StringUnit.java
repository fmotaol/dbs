package core.parsing.tree;

import java.util.regex.Pattern;

public interface StringUnit extends StringItem {

	String[] split(Pattern patternOpenOrClose);
	
	
}