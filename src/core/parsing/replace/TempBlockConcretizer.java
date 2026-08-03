package core.parsing.replace;

import java.util.regex.Pattern;

import core.parsing.tree.EnclosedBlock;
import core.parsing.tree.StringItem;
import core.parsing.tree.CompositeString;
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
		CompositeString tree = parseTree(ss);
		StringUnit r = tree.convertToBlock();
		//falta concretizar
		return r;
	}
	
	private record IndexedEnclosedBlock (EnclosedBlock block, int start, int end) {};
	
	private IndexedEnclosedBlock parseSubBlock(String[] parts, int from) {
		int start = from + 1;
		int bstart = from + 1; //bloco "before"
		int bend = parts.length - 1; //bloco "before"
		CompositeString b = new CompositeString();
		do {
			start = find(parts, start, patternOpen);
			if (start > 0) {
				IndexedEnclosedBlock ie = parseSubBlock(parts, start);
				
				bend = start - 1;
				for (int i = bstart; i <= bend; i++)
					b.add(parts[i]);
			
				b.add(ie.block());
				bstart = ie.end;
			}
			
		} while (start < parts.length - 1 && start > 0);
		
		if (start > 0) {
			for (int i = from + 1; i <= bend; i++)
				b.add(parts[i]);
		}
		EnclosedBlock c = new EnclosedBlock(parts[0], b, parts[parts.length - 1]);
		return new IndexedEnclosedBlock(c, );
	}

	private int find(String[] parts, int from, Pattern pattern) {
		throw new RuntimeException("ainda não implementado");
	}

	private boolean match(String s, Pattern pattern) {
		throw new RuntimeException("ainda não implementado");
	}

}
