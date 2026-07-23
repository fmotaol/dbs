package core.dataset;

public class RowIdMarkPoint implements MarkPoint {
	
	private int rowId;

	public RowIdMarkPoint(int rowId) {
		super();
		this.rowId = rowId;
	}

	public int getRowId() {
		return rowId;
	}

	@Override
	public int compareTo(Record record) {
		int rid = record.getRowId();
		return rowId - rid;
	}

	@Override
	public String toString() {
		return "[rowId=" + rowId + "]";
	}

}
