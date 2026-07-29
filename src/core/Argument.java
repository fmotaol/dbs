package core;

import util.Util;
import util.logical.Check;

public class Argument {

	public DBS program;

	private String name = null;

	public String label = null;

	private String value = null;

	public String[] valueList = null;

	public String defaultValue = null;

	public boolean useDefault = false;

	public Origin origin = null;

	public UndefinedAction undefinedAction = UndefinedAction.ERROR;

	public UndefinedAction getUndefinedAction() {
		return undefinedAction;
	}

	public enum UndefinedAction {
		ERROR, IGNORE, NULL, ASK;

		static UndefinedAction parse(String text) {
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
	};

	public Argument(DBS program) {
		this.program = program;
	}

	public void setUndefinedAction(UndefinedAction action) {
		this.undefinedAction = action;
	}

	public void setUndefinedValueAction(String action) {
		this.undefinedAction = UndefinedAction.parse(action);
	}
	
	public int index() {
		return program.arguments.indexOf(this);
	}

	public boolean allowedValue(String value, boolean informError) {
		if (valueList == null)
			return true;
		boolean r = Util.contains(valueList, value);
		if (!r && informError) {
			System.out.println("Valor " + value + " não permitido em " + name);
			System.out.println("Os valores permitidos são: " + Util.concat(valueList, ", "));
		}
		return r;
	}

	public void obtainArg() {
		if (useDefault) {
			if (defaultValue == null)
				throw new RuntimeException("Não foi definido valor default para o argumento " + name);
			setDefaultValue();
			return;
		}

		switch (undefinedAction) {
			case ERROR: 
				throw new RuntimeException("Não foi definido valor para o argumento " + name);
	
			case IGNORE: 
				return;
					
			case NULL: {
				this.value = null;
				return;
			}
	
			case ASK: {
				requestUser();
			}
		}
	}

	public void requestUser() {
		do {
			internalRequestArg();
		} while (!allowedValue(value, true));
	}
	
	public String label() {
		String r = label;
		if (r == null)
			r = name;
		if (r == null)
			r = index() + "";
		return r;
	}

	private void internalRequestArg() {

		String defaultExp = "";
		if (defaultValue != null) {
			defaultExp = " (default " + defaultValue + ")";
		}

		value = DBS.inputQuery("Informe o valor para o argumento \"" + label() + "\"" + defaultExp + ": ");
		origin = Origin.USER;

		if (defaultValue != null) {
			if ((value == null) || ("".equals(value))) {
				System.out.println("Assumido valor default " + defaultValue);
				value = defaultValue;
				origin = Origin.DEFAULT;
			}
		}
	}

	public String getValue() {
		if (value == null)
			obtainArg();
		return value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
//		if (Util.isNumber(name))
//			throw new RuntimeException("Nome de argumento n�o pode ser puramente num�rico");
		this.name = name;
	}

	public void setValue(String value, Origin origin) {
		if (!allowedValue(value, true))
			throw new RuntimeException("Argumento inválido");
		this.value = value;
	}

	public static enum Origin {
		PROGRAM_INPUT, USER, FORCED, ARG_FILE, SAV_FILE, DEFAULT
	}

	@Override
	public String toString() {
		return getFullId() + " = " + Check.coalesce(value, "null");
	}

	public String getId() {
		if (name != null)
			return name;
		return index() + "";
	};

	public String getFullId() {
		String id = getId();
		return "arg[" + id + "]";
	};

	public Origin getOrigin() {
		if (origin == null) {
			getValue(); // força a obter o valor
		}
		return origin;
	}

	public void setDefaultValue() {
		value = defaultValue;
		origin = Origin.DEFAULT;
	}

	public boolean shouldIgnore() {
		return isUndefined() && undefinedAction == UndefinedAction.IGNORE;
	}

	public boolean isUndefined() {
		return value == null;
	}

}
