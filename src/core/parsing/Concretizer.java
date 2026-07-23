package core.parsing;

import core.performer.Performer;

public abstract class Concretizer {
	
	protected Performer performer;
	protected boolean recursiveReference;
	
	public Concretizer(Performer performer) {
		super();
		this.performer = performer;
	}

	public boolean isRecursiveReference() {
		return recursiveReference;
	}

	public void setRecursiveReference(boolean recursiveReference) {
		this.recursiveReference = recursiveReference;
	}
	
}
