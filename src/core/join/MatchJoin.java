package core.join;

import java.util.concurrent.Future;

import core.dataset.DataSet;
import core.performer.Context;
import core.performer.Join;
import core.performer.SourcePerformer;
import util.Maths;

public abstract class MatchJoin extends Join {

	public enum MatchType {
		MATCH, MISS, OUTER
	};

	protected String[] upperKeyTemplate;

	protected String[] lowerKeyTemplate;

//	TODO para o ASYNC, associar estas variáveis ao DataSet
	protected KeySource upperKey;
	protected KeySource lowerKey;
//	protected MatchType matchTrigger;
	protected Future<DataSet> futureDataSetBuffer;

	public MatchJoin(SourcePerformer lower) {
		super(lower);
	}

	public void setKeyDefinition(String[] upperKey, String[] lowerKey) {
		upperKeyTemplate = upperKey;
		lowerKeyTemplate = lowerKey;
	}

	public int compare(String val1, String val2) {
		if (Maths.isNumeric(val1) && Maths.isNumeric(val2))
			return compareAsNumbers(val1, val2);

		throw new RuntimeException("ainda não implementado");
	}

	private int compareAsNumbers(String val1, String val2) {
		float v1 = Float.parseFloat(val1);
		float v2 = Float.parseFloat(val1);
		if (v1 > v2)
			return +1;
		if (v1 < v2)
			return -1;

		return 0;
	}

	public MatchType matchType(KeySource upper, KeySource lower) {
		if (upper.currentKeyIsNull() || lower.currentKeyIsNull())
			return null;
//			throw new RuntimeException("Chave do join não pode ser nula");
		
		if (upper.hasDifferentSortDirection(lower))
			throw new RuntimeException("Datasets possuem sentido de ordenação opostos");

		if (upper.matchCurrentKey(lower))
			return MatchType.MATCH;

		if (upper.missedCurrentKeyIn(lower))
			return MatchType.MISS;

		if (lower.missedCurrentKeyIn(upper))
			return MatchType.OUTER;

		return null;
	}

	@Override
	protected Future<DataSet> requestDataSet(String templateSQL, Context context) {
		return futureDataSetBuffer;
	}

	@Override
	public void invokerRequestedDataSet(SourcePerformer invoker, Context context, Future<DataSet> loadingDataSet) {
		if (invoker != lower.getInvoker())
			throw new RuntimeException("Situação a ser tratada");
		futureDataSetBuffer = super.requestDataSet(lower.getTemplateCommand(context), context);

		upperKey = new DataSetKeySource(invoker, upperKeyTemplate, loadingDataSet);
		lowerKey = new DataSetKeySource(lower, lowerKeyTemplate, futureDataSetBuffer);

	}

	@Override
	public Context suitContext(Context context) {
		MatchType m = matchType(upperKey, lowerKey);
		if (m == null)
			return context;

		if (m == MatchType.MATCH)
			return context;

		if (m == MatchType.MISS)
			return context.nullValuesCopy();

		if (m == MatchType.OUTER) {
			Context r = nullValuesParentContext(context);
			return r;
		}

		throw new RuntimeException("Erro interno");
	}

	private Context nullValuesParentContext(Context context) {
		Context parent = context.getParent();
		parent = parent.nullValuesCopy();
		Context c = new Context(parent, context.record.createBuffer(), context.dataSet);
		c.setParent(parent);
		return c;
	}

	public MatchType getMatchTrigger() {
		return matchType(upperKey, lowerKey);
	}

	@Override
	protected boolean next(DataSet dataSet, Context invokerContext) {
		boolean r = internalNext(dataSet, invokerContext);
		notifySlavesJumpedNext(dataSet, invokerContext);
		return r;
	}
	
	private int matchedRecords = 0;
	private int missedRecords = 0;
	private int outerRecords = 0;

	@Override
	public void resetRecordsFound() {
		matchedRecords = 0;
		missedRecords = 0;
		outerRecords = 0;
	}
	
	protected void incRecords(MatchType matchType, int inc) {
		if (matchType == MatchType.MATCH)
			matchedRecords += inc;
		else if (matchType == MatchType.MISS)
			missedRecords += inc;
		else if (matchType == MatchType.OUTER)
			outerRecords += inc;
	}

	@Override
	protected void incRecordCount(int inc) {
		MatchType matchType = matchType(upperKey, lowerKey);
		incRecords(matchType, inc);
	}
	
	@Override
	protected void reportCounter() {
		lower.reportIdentifiedRecords("coincidente", matchedRecords, false, "(MATCH)");
		lower.reportIdentifiedRecords("faltante", missedRecords, false, "(MISS)");
		lower.reportIdentifiedRecords("espurio", outerRecords, false, "(OUTER)");
	}

	protected abstract boolean internalNext(DataSet dataSet, Context invokerContext);


}
