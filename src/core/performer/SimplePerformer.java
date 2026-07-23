package core.performer;

public interface SimplePerformer {

	Result perform(String templateSQL, Context context);

	void showTree();

}
