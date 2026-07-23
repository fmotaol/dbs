package core.performer;

import java.util.ArrayList;
import java.util.List;

import core.parsing.expression.Expression;

public class ConditionalCommand extends TemplateCommand {

	private StaticCommand defaultCommand;

	private List<Alternative> alternatives = new ArrayList<>();

	public StaticCommand getDefaultCommand() {
		return defaultCommand;
	}

	public void setDefaultCommand(StaticCommand defaultCommand) {
		this.defaultCommand = defaultCommand;
	}

	public ConditionalCommand(Performer owner) {
		super(owner);
	}

	public StaticCommand activatedCommand(Context context) {
		for (Alternative a : alternatives) {
			if (a.satisfy(context))
				return a.command;
		}
		if (defaultCommand != null)
			return defaultCommand;

		return null;
	}

	@Override
	public String getCommand(Context context) {
		StaticCommand c = activatedCommand(context);
		if (c != null)
			return c.getCommand();
		else
			return null;
//			throw new RuntimeException("Nenhum comando SQL foi ativado com a condição atual: " + context);
	}

	public void addAlternative(String command, Expression<Boolean> condition) {
		Performer o = getOwner();
		if (condition != null) { 
			Alternative a = new Alternative(o, condition, command);
			alternatives.add(a);			
		} else {// é o comando do #else
			if (defaultCommand != null)
				throw new RuntimeException("Já existe declaração de \"else\" neste bloco");
			setDefaultCommand(new StaticCommand(o, command));
		}
	}

	private class Alternative {

		private Expression<Boolean> condition;

		private StaticCommand command;

		public boolean satisfy(Context context) {
			Expression<Boolean> exp = condition.copy();
			exp.concretize(getOwner().defaultConcretizer, context);
			return exp.solve();
		}

		public Alternative(Performer owner, Expression<Boolean> condition, String command) {
			super();
			this.condition = condition;
			this.command = new StaticCommand(owner, command);
		}

	}

	@Override
	public String toString() {
		return alternatives.size() + " alternativas de comando";
	}

	@Override
	public void showTree(String prefix) {
		for (Alternative a : alternatives) {
			StaticCommand c = a.command;
			c.showTree(prefix + "when " + a.condition + ": ");
		}
		if (defaultCommand != null)
			defaultCommand.showTree(prefix + "else: ");
	}

}
