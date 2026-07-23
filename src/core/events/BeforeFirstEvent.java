package core.events;

import core.dataset.DataSet;
import core.performer.Context;

public class BeforeFirstEvent extends RowNumberEvent {
	
	public BeforeFirstEvent() {
		super();
	}

	@Override
	public boolean checkEachRow(Context context) {
		return false;
	}

	@Override
	public boolean checkBeforeFirstRow(DataSet source) {
		return true;
	}


}
