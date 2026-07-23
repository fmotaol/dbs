package core.performer;

public abstract class TemplateCommand {
	
	private Performer owner;

	public abstract String getCommand(Context context);

	public TemplateCommand(Performer owner) {
		super();
		this.owner = owner;
	}

	public Performer getOwner() {
		return owner;
	}

	public abstract void showTree(String prefix);

}
