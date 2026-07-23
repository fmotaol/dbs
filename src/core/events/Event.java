package core.events;

import java.util.HashMap;

import core.dataset.DataSet;
import core.join.MatchEvent;
import core.join.MissEvent;
import core.join.OuterEvent;
import core.parsing.replace.StringConcretizer;
import core.performer.Context;
import core.performer.SourcePerformer;

public abstract class Event {

	protected SourcePerformer source;

	public SourcePerformer getSource() {
		return source;
	}

	public void setSource(SourcePerformer source) {
		this.source = source;
	}

	public Event() {
		super();
	}

	public static final String[] KNOWN_EVENT_NAMES = { "beforefirst", "onlyfirst", "exceptfirst", "newvalue",
			"splitrows", "valuechanged", "onlylast", "exceptlast", "afterlast", "onmatch", "onmiss", "onouter" };

	private static final HashMap<String, Class<? extends Event>> managedEventClasses = new HashMap<String, Class<? extends Event>>();

	static {
		managedEventClasses.put("beforefirst", BeforeFirstEvent.class);
		managedEventClasses.put("onlyfirst", OnlyFirstEvent.class);
		managedEventClasses.put("exceptfirst", ExceptFirstEvent.class);
//		managedEventClasses.put("if", IfEvent.class);
//		managedEventClasses.put("else", ElseEvent.class);
		managedEventClasses.put("newvalue", NewValueEvent.class);
		managedEventClasses.put("valuechanged", ValueChangedEvent.class);
		managedEventClasses.put("splitrows", SplitRowsEvent.class);
		managedEventClasses.put("exceptlast", ExceptLastEvent.class);
		managedEventClasses.put("onlylast", OnlyLastEvent.class);
		managedEventClasses.put("afterlast", AfterLastEvent.class);
		managedEventClasses.put("match", MatchEvent.class);
		managedEventClasses.put("miss", MissEvent.class);
		managedEventClasses.put("outer", OuterEvent.class);
	}

	public static Event create(String eventType, SourcePerformer source) {
		if (eventType == null)
			throw new RuntimeException("Tipo de evento nulo");

		Class<? extends Event> clazz = managedEventClasses.get(eventType);
		if (clazz == null)
			throw new RuntimeException("Tipo de evento desconhecido: " + eventType);
		try {

			Event r = clazz.newInstance();
			r.setSource(source);
			return r;

		} catch (IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		}
	}

	public void setParams(String params) {
		// nada aqui
	}

	public abstract boolean checkEachRow(Context context);

	public boolean checkAfterLastRow(DataSet source) {
		return false;
	}

	public boolean checkBeforeFirstRow(DataSet source) {
		return false;
	}

	public abstract boolean isIterableOnRows();

//	public boolean is(Class<? extends Event> eventType) {
//		return this instanceof eventType.class;
//	}

	public static boolean isKnown(String name) {
		return managedEventClasses.containsKey(name);
	}

//	protected final String concretize(String exp, DataSet dataSet) {
//		return concretize(exp, dataSet, null);
//	}

	protected String concretize(String exp, Context context) {
		StringConcretizer concretizer = getSource().getDefaultConcretizer();
		String s = concretizer.concretizeAll(exp, context);
		return s;
	}

	public String toString() {
		String r = getClass().getName();
		String d = getEventDetailsString();
		if (!"".equals(d))
			r += " " + d;
		return r;
	}

	protected String getEventDetailsString() {
		return "";
	}

	protected StringConcretizer getConcretizer() {
		return getSource().getDefaultConcretizer();
	}

}
