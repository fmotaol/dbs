package core.events;

import core.dataset.DataSet;

public abstract class RowNumberEvent extends Event {

	public RowNumberEvent() {
		super();
	}

	protected int rowId(DataSet source) {
		return source.getRowId();
	}


	@Override
	public boolean isIterableOnRows() {
		return true;
	}

}
