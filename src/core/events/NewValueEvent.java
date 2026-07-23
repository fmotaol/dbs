package core.events;

import java.util.HashSet;

import core.performer.Context;

public class NewValueEvent extends ValueEvent {

	public NewValueEvent() {
		super();
	}

	private HashSet<String> passedValues = new HashSet<String>();

	@Override
	public boolean checkEachRow(Context context) {
		String value = concreteValue(context);

		boolean r = passedValues.contains(value);
		if (!r)
			passedValues.add(value);

		return !r;
	}

}
