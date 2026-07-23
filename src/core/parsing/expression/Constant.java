package core.parsing.expression;

import core.parsing.replace.StringConcretizer;
import core.performer.Context;

public class Constant<T> implements Expression<T> {
	
	private T value;

	public Constant(T value) {
		this.value = value;
	}

	@Override
	public Expression<T> copy() {
		return new Constant<T>(value);
	}

	@Override
	public void concretize(StringConcretizer c, Context context) {
	}

	@Override
	public T solve() {
		return value;
	}

}
