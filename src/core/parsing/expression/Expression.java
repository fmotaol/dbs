package core.parsing.expression;

import core.parsing.replace.StringConcretizer;
import core.performer.Context;

public interface Expression<T> {

	public Expression<T> copy();

	public void concretize(StringConcretizer c, Context context);

	public T solve();

}
