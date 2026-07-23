package core.events;

import core.performer.Context;

public class OnlyLastEvent extends RowNumberEvent {

	public OnlyLastEvent() {
		super();
	}

	@Override
	public boolean checkEachRow(Context context) {
		return !context.dataSet.hasNext();
	}

}
