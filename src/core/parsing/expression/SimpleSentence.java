package core.parsing.expression;

import core.parsing.replace.StringConcretizer;
import core.performer.Context;

public abstract class SimpleSentence<T> implements Expression<T> {

	protected String sentence;

	public SimpleSentence(String sentence) {
		this.sentence = sentence;
	}

	@Override
	public void concretize(StringConcretizer c, Context context) {
		sentence = c.concretizeAll(sentence, context);
	}

}
