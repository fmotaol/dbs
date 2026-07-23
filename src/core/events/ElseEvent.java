package core.events;

import core.performer.Context;

@Deprecated
public class ElseEvent extends Event {
	
	private IfEvent ifEvent;

	@Override
	public boolean checkEachRow(Context context) {
		if (ifEvent == null)
			throw new RuntimeException("Evento 'else' sem 'if' associado");
		return !ifEvent.checkEachRow(context);
	}

	public IfEvent getIfEvent() {
		return ifEvent;
	}

	public void setIfEvent(IfEvent ifEvent) {
		this.ifEvent = ifEvent;
	}

	@Override
	public boolean isIterableOnRows() {
		return true;
	}

}
