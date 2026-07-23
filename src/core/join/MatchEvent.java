package core.join;

import core.join.MatchJoin.MatchType;

public class MatchEvent extends JoinEvent {

	public MatchEvent() {
		super();
		setMatchType(MatchType.MATCH);
	}

}
