package core.events;

import core.parsing.expression.Expression;
import core.performer.Context;

@Deprecated
public class IfEvent extends Event {
	
	private Expression<Boolean> expression;

	@Override
	public void setParams(String params) {
		throw new RuntimeException("Chamada inválida");
//		expression = new Comparison(params);
	}

	@Override
	public boolean isIterableOnRows() {
		return true;
	}

	public IfEvent() {
		super();
	}

	@Override
	public boolean checkEachRow(Context context) {
		
		Expression<Boolean> e = expression.copy();
		e.concretize(getConcretizer(), context);
		return e.solve();
		
	}

}
