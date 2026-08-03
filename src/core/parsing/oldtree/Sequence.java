package core.parsing.oldtree;

import java.util.ArrayList;

import core.performer.Context;

public class Sequence extends Node {
	
	private ArrayList<Node> subNodes = new ArrayList<Node>();

	@Override
	public String concretize(Context context) {
		StringBuilder sb = new StringBuilder();
		for (Node n : subNodes) {
			String s = n.concretize(context);
			sb.append(s);
		}
		return sb.toString();
	}

}
