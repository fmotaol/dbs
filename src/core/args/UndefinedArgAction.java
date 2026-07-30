package core.args;

import core.DBS;

public enum UndefinedArgAction {
	ERROR, IGNORE, NULL, ASK;

	public static UndefinedArgAction parse(String text) {
		if (text.equalsIgnoreCase("ERROR"))
			return ERROR;
		if (text.equalsIgnoreCase("IGNORE"))
			return IGNORE;
		if (text.equalsIgnoreCase("NULL"))
			return NULL;
		if (text.equalsIgnoreCase("ASK"))
			return ASK;
		throw new RuntimeException("Opção inválida: " + text);
	}
	
	public static Argument argumentFromAction(DBS program, String name, UndefinedArgAction undefinedAction) {
		Argument a = program.getArgByName(name);
		if (a != null)
			return a;

		if (undefinedAction == UndefinedArgAction.ERROR)
			throw new RuntimeException("Não foi definido valor para o argumento " + name);

		if (undefinedAction == UndefinedArgAction.IGNORE)
			return null;

		a = program.createArg(name);

		if (undefinedAction == UndefinedArgAction.NULL)
			return a; // já inicializa com value null

		if (undefinedAction == UndefinedArgAction.ASK)
			a.requestUser();

		return a;
	}

	
};

