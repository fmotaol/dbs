package core.parsing.string;

import java.util.function.Predicate;

public interface Transaction {

	public enum State {
		ACTIVE, COMMITED, CANCELLED
	};

	public State getState();

	public Predicate<String> getValueCriteria();

	public void commit();

	public void rollback();
	

}
