package core.join;

import core.join.MatchJoin.MatchType;

public class OuterEvent extends JoinEvent {

	public OuterEvent() {
		super();
		setMatchType(MatchType.OUTER);
	}

}
