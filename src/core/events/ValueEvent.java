package core.events;

import core.dataset.DataSet;
import core.dataset.Record;
import core.performer.Context;

public abstract class ValueEvent extends Event {

	public ValueEvent() {
		super();
	}

	@Override
	public final void setParams(String params) {
		template = params;
	}

	@Override
	public final boolean isIterableOnRows() {
		return true;
	}

	private String template;

//	protected final String concreteValue(DataSet source) throws SQLException, ParseException, IOException {
//		return concreteValue(source, null);
//	}

	protected String concreteValue(Context context) {
		String s = getConcretizer().concretizeAll(template, context);
		return s;
	}

//	protected boolean equalsWhenConcretized(DataSet source) {
//		Record v1 = source.currentRecord();
//		Record v2 = source.previousRecord();
//		return equalsWhenConcretized(source, v1, v2);
//	}

	protected boolean equalsWhenConcretized(Context parent, DataSet source, Record r1, Record r2) {
		String sc = concreteValue(new Context(parent, r1, source));
		String sp = concreteValue(new Context(parent, r2, source));
		return sc.equals(sp);
	}

}
