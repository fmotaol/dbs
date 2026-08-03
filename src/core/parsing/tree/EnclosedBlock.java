package core.parsing.tree;

public class EnclosedBlock implements StringItem {
	
	private StringUnit open;
	private StringItem block;
	private StringUnit close;

	public EnclosedBlock(String open, StringItem block, String close) {
		super();
		this.open = new StringUnit(open);
		this.block = block;
		this.close = new StringUnit(close);
	}
	
	public EnclosedBlock(StringUnit open, StringItem block, StringUnit close) {
		super();
		this.open = open;
		this.block = block;
		this.close = close;
	}
	
	public StringItem[] getItems() {
		return new StringItem[] {open, block, close};
	}

	public StringUnit getOpen() {
		return open;
	}

	public StringItem getBlock() {
		return block;
	}

	public StringUnit getClose() {
		return close;
	}

	
}
