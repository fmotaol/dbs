package core.parsing.expression;

import core.parsing.replace.StringConcretizer;
import core.performer.Context;
import util.Util;

public class Comparison implements Expression<Boolean> {
	

	@Override
	public String toString() {
		return "Comparison [" + operand1 + " " + operator + " " + operand2 + "]";
	}

	private String operand1;
	private String operator;
	private String operand2;

	public Comparison(String operand1, String operator, String operand2) {
		this.operand1 = operand1;
		this.operator = operator;
		this.operand2 = operand2;
	}

	@Override
	public void concretize(StringConcretizer c, Context context) {
		operand1 = c.concretizeAll(operand1, context);
		operand2 = c.concretizeAll(operand2, context);
//		Comparison r = new Comparison(o1, operator, o2);
	}

	@Override
	public Boolean solve() {
		if ((operand1 == null) && (operator.equalsIgnoreCase("is")) && (operand2.equalsIgnoreCase("null")))
			return true;
		
		if ((operand1 == null) || (operator == null) || (operand2 == null))
			return false;
		
		try {
			float n1 = Float.parseFloat(operand1);
			float n2 = Float.parseFloat(operand2);
			
			return Util.compare(n1, operator, n2);
			
		} catch (NumberFormatException e) {
			//Não são numéricos
		}
		
		return Util.compare(operand1, operator, operand2);
	}

	@Override
	public Comparison copy() {
		Comparison r = new Comparison(operand1, operator, operand2);
		return r;
	}


}
