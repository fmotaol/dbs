package core.events;

import core.dataset.DataSet;
import core.performer.Context;

public class AfterLastEvent extends RowNumberEvent {

	public AfterLastEvent() {
		super();
	}

	@Override
	public boolean checkEachRow(Context context) {
		return false;
	}

	@Override
	public boolean checkAfterLastRow(DataSet source) {
		return true;
	}

}
