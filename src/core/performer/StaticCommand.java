package core.performer;

public class StaticCommand extends TemplateCommand {
	
	private String command;

	public StaticCommand(Performer owner, String command) {
		super(owner);
		if (command == null)
			throw new RuntimeException("Comando nulo");
		this.command = command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	@Override
	public String toString() {
		return command;
	}

	public String getCommand() {
		return command;
	}

	@Override
	public String getCommand(Context context) {
		return command;
	}

	@Override
	public void showTree(String prefix) {
		String preview = "";
		if (command.length() > 100)
		preview = " " + command.substring(1, 100) + "...";
	else
		preview = " " + command;
		System.out.println(prefix + preview);
	}

}
