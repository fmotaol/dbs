package core.parsing.tree;

import java.util.ArrayList;
import java.util.List;

import util.Colls;

public class CompositeString implements StringItem {
	
	private List<StringItem> items;
	
	public CompositeString() {
		this.items = new ArrayList<StringItem>();
	}

	public CompositeString(StringItem[] items) {
		this.items = Colls.toList(items);
	}

	public void add(StringItem item) {
		 this.items.add(item);
	}

	public void add(String s) {
		add(new StringUnit(s));
	}

	public StringUnit convertToBlock() {
		String s = this.toString();
		return new StringUnit(s);
	}
	
}
