package core.events;

import core.performer.Context;

public class ExceptFirstEvent extends RowNumberEvent {

	public ExceptFirstEvent() {
		super();
	}

	@Override
	public boolean checkEachRow(Context context) {
		return rowId(context.dataSet) > 1;
	}

}
