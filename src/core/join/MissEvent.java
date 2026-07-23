package core.join;

import core.join.MatchJoin.MatchType;

public class MissEvent extends JoinEvent {

	public MissEvent() {
		super();
		setMatchType(MatchType.MISS);
	}

}
