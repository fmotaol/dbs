package core.parsing.replace;

import java.util.regex.Pattern;

import core.parsing.tree.EnclosedBlock;
import core.parsing.tree.StringItem;
import core.parsing.tree.StringTree;
import core.parsing.tree.StringUnit;
import core.performer.Context;
import core.performer.Performer;
import util.logical.Assert;

public class TempBlockConcretizer {
	
	String regexOpen = "????";
	String regexClose = "????";
	String regexOpenOrClose = "????";
	Pattern patternOpen = Pattern.compile(regexOpen);
	Pattern patternClose = Pattern.compile(regexClose);
	Pattern patternOpenOrClose = Pattern.compile(regexOpenOrClose);

	private String extract(String[] array, int index) {
		if (index < 0 || index >= array.length)
			return null;
		return array[index];
	}
	
	public StringUnit concretizeSubBlock(StringUnit sql, Performer performer, Context context) {
		String[] ss = sql.split(regexOpenOrClose);
		StringTree tree = parseTree(ss);
		StringUnit r = tree.convertToBlock();
		//falta concretizar
		return r;
	}

	
	private record IndexedEnclosedBlock (EnclosedBlock block, int start, int end) {};
	
	private IndexedEnclosedBlock parseSubBlock(String[] parts, int from) {
		int start = find(parts, from + 1, patternOpen);		
		int bend = parts.length - 1;
		if (start > 0) {
			IndexedEnclosedBlock ie = parseSubBlock(parts, start);
			bend = start - 1;
		}
		
		int end = find(parts, from, patternClose);
		if (end > 0) {
			addAll(r, parts, from, end);
		}
		
		return null;
	}

	private int find(String[] parts, int from, Pattern pattern) {
		throw new RuntimeException("ainda não implementado");
	}

	private boolean match(String s, Pattern pattern) {
		throw new RuntimeException("ainda não implementado");
	}

}
