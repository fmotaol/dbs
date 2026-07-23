package core.events;

import core.dataset.Record;
import core.performer.Context;

public class ValueChangedEvent extends ValueEvent {

	public ValueChangedEvent() {
		super();
	}

	@Override
	public boolean checkEachRow(Context context) {
		Record c = context.dataSet.currentRecord(); //deveria pegar do parâmetro record
		Record p = context.dataSet.previousRecord();
		if (!equalsWhenConcretized(context.parent, context.dataSet, c, p))
			return true;
		
		return false;
	}

}
