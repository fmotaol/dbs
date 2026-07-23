package core.parsing.expression;

public class BooleanSentence extends SimpleSentence<Boolean> {

	public BooleanSentence(String sentence) {
		super(sentence);
	}

	@Override
	public Expression<Boolean> copy() {
		return new BooleanSentence(sentence);
	}

	@Override
	public Boolean solve() {
		if (sentence == null)
			return null;
		return Boolean.parseBoolean(sentence);
	}

}
