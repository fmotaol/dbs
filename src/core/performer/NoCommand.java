package core.performer;

public class NoCommand extends TemplateCommand {

	public NoCommand(Performer owner) {
		super(owner);
	}

	@Override
	public String getCommand(Context context) {
		throw new RuntimeException("Não foi definido template command para este performer");
	}

	@Override
	public void showTree(String prefix) {
		System.out.println(prefix + "(no command)");
	}

}
