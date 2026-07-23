package core.events;

import core.performer.Context;

public class ExceptLastEvent extends RowNumberEvent {

	public ExceptLastEvent() {
		super();
	}

	@Override
	public boolean checkEachRow(Context context) {
		return context.dataSet.hasNext();
	}

}
