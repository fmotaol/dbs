package core.parsing.oldtree;

import core.performer.Context;

public class SQLConstant extends Node {
	
	private String constantSQL;

	@Override
	public String concretize(Context context) {
		return constantSQL;
	}

}
