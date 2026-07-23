package core.join;

import core.events.Event;
import core.join.MatchJoin.MatchType;
import core.performer.Context;

public class JoinEvent extends Event {
	
	private MatchType matchType;

	public JoinEvent() {
		super();
	}

	public MatchType getMatchType() {
		return matchType;
	}

	protected void setMatchType(MatchType matchType) {
		this.matchType = matchType;
	}

	@Override
	public boolean checkEachRow(Context context) {
		MatchType trigger = getMatchTrigger();
		if (trigger == matchType)
			return true;
		else
			return false;
	}
	
	@Override
	public boolean isIterableOnRows() {
		return true;
	}

	protected MatchJoin getJoin() {
		return (MatchJoin) source.getJoinType();
	}
	
	protected MatchType getMatchTrigger() {
		return getJoin().getMatchTrigger();
	}
	
}
