package core.parsing.expression;

import java.util.Arrays;

import core.parsing.replace.StringConcretizer;
import core.performer.Context;
import util.Colls;


public class In implements Expression<Boolean> {

	private String element;
	
	private String[] list;

	@Override
	public Expression<Boolean> copy() {
		In r = new In(element, Arrays.copyOf(list, list.length));
		return r;
	}

	public In(String element, String[] list) {
		super();
		this.element = element;
		this.list = list;
	}

	@Override
	public void concretize(StringConcretizer c, Context context) {
		element = c.concretizeAll(element, context);
		list = Colls.transform(list, (e) -> c.concretizeAll(e));
	}

	@Override
	public Boolean solve() {
		for (String s : list) {
			if (element.equals(s))
				return true;
		}
		return false;
	}

	public String getElement() {
		return element;
	}

	public String[] getList() {
		return list;
	}

	@Override
	public String toString() {
		return "In [" + element + " in (" + Arrays.toString(list) + ")";
	}

}
