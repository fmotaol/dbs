package core.parsing.tree;

import core.performer.Context;

public abstract class Node {
	
	protected TreeConcretizer engine;
	
	public abstract String concretize(Context context);
	
}
