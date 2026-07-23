package core.events;

import core.performer.Context;

public class OnlyFirstEvent extends RowNumberEvent {

	public OnlyFirstEvent() {
		super();
	}

	@Override
	public boolean checkEachRow(Context context) {
		return rowId(context.dataSet) == 1;
	}

}
