package core.parsing.tree;

import core.performer.Context;

public class SQLConstant extends Node {
	
	private String constantSQL;

	@Override
	public String concretize(Context context) {
		return constantSQL;
	}

}
