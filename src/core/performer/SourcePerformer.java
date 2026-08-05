package core.performer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import core.DBS;
import core.Device;
import core.Macro;
import core.dataset.DataSet;
import core.dataset.Field;
import core.dataset.Record;
import core.events.Event;
import core.join.Cartesian;
import core.join.FollowerJoin;
import core.join.MatchJoin;
import core.parsing.CommandParser;
import core.savepoint.SavePoint;
import core.savepoint.SavePointRestoreable;
import core.sql.Language;
import util.Util;
import util.logical.Assert;

public class SourcePerformer extends Performer implements SavePointRestoreable {

	private boolean repeat = false;

	private boolean repeatWhileFound = false;

	private boolean repeatWhileSubFound = false;

	public SourcePerformer(Macro engine, String connectionId, SourcePerformer invoker) {
		super(engine, connectionId, invoker);
	}

	private ArrayList<SlavePerformer> slaves = new ArrayList<SlavePerformer>();

	private Boolean hasIterationSlaves;

	private DataSet dataSetInUse;

	private boolean asyncIteration = false;

	boolean asyncLoading = false;

	@Override
	public synchronized Result perform(String templateSQL, Context invokerContext) {

		String id = "";

		DataSet dataSet = null;

		do {

			if (join.isNewExecution()) {

				resetRecordsFound();

				showStackCurrentScene(0, invokerContext);

				delay(delayTimeBefore);

				showLabel(invokerContext);

				System.out.println();

				totalOperations++;

			}

			dataSet = requestDataSet(templateSQL, invokerContext);

//			if (invokerContext != null)
//				dataSet.setInvokerContext(invokerContext);

			dataSetInUse = dataSet;

			iterate(dataSet, invokerContext, asyncIteration);

			if (join.finishedExecution()) {

				if (repeat)
					println("Executando novamente...");

				dataSet.close();

				if (id.isEmpty())
					id = "global";

				if (showStatus)
					reportCounter();
				if (showPerformance())
					checkForReportPerformance();

				delay(delayTimeAfter);

				Macro.println();

			}

		} while (repeat);

		return new Result(0, dataSet);
	}

	public boolean subFoundRecords() {
		for (SlavePerformer s : slaves) {
			if (s.foundRecords())
				return true;
		}
		return false;
	}

	public void resetRecordsFound() {
		join.resetRecordsFound();
		resetSlavesRecordFound();
	}

	private void resetSlavesRecordFound() {
		for (SlavePerformer s : slaves) {
			s.resetRecordsFound();
		}
	}

	private DataSet requestDataSet(String templateSQL, Context invokerContext) {
		if (templateSQL == null)
			throw new RuntimeException("Imposs�vel buscar DataSet de um comando SQL vazio");

		DataSet r;
		Future<DataSet> fds = join.requestDataSet(templateSQL, invokerContext);
		try {
			r = fds.get();
		} catch (InterruptedException | ExecutionException e) {
			Util.throwAsRuntimeException(e);
			throw new RuntimeException(e);
		}
		return r;
	}

	void notifySlavesDataSetRequested(SourcePerformer invoker, Context invokerContext, Future<DataSet> loadingDataSet) {
		for (SlavePerformer s : slaves) {
			s.notifyInvokerRequestedDataSet(invoker, invokerContext, loadingDataSet);
		}
	}

	void iterate(DataSet dataSet, Context invokerContext, boolean async) {
		if (async) {
			DBS.parallelizer().run(() -> iterate(dataSet, invokerContext));
		} else {
			iterate(dataSet, invokerContext);
		}
	}

	private void iterate(DataSet dataSet, Context invokerContext) {

		Date t = new Date();

		if (execution == 0) {
			incExecution(1);

			if (showDataRow() && firstRowIsHeader())
				showHeader("-> ", dataSet);

			if (getTotalRecordCount() == 0)
				performDefaultStartImportingData(slaves, dataSet, invokerContext);
		} else
			incExecution(1);
			

		dispatchBeforeFirstEvents(invokerContext);

		Context context = new Context(invokerContext, null, dataSet);

		while (join.next(dataSet, invokerContext)) {

			context.saveRecordAsPrevious();
			context.record = dataSet.currentRecord();
			internalIncRecordCount(context.record);

			boolean sk = seekForSavePoint(context.record); // restaura o savePoint
			if (sk)
				continue;

			if (savePointColumns != null) { // registra o savePoint
				registerSavePoint(context.record);
			}

			if (join.shouldPerformSlaves(context))
				performSlaves(context);

			if (showDataRow()) {
				showTableRecord("-> ", context.record);
			}

			
		}

		if (join.finishedExecution()) { // difere do sourceNext == false quando num join FOLLOWER

			if (dataSet.getRowId() > 1) {

				sendTargetBatches(invokerContext);
				dispatchAfterLastEvents(invokerContext);
				if (invoker == null)
					performDefaultEndImportingData(slaves, dataSet, invokerContext);
			}

			setRepeat(false); //necess�rio aqui, porque no pr�ximo comando pode conter "repeat" definido pelo usu�rio
			executeActionsBasedOnRecordsFound(invokerContext);
			
			checkForEnableRepeatMode();

			registerSavePointAsDone();

			setAsDone(false); // para rein�cio

			if (showStatus) {
				showExecutionStatus(t);
			}
		}

	}

	private void checkForEnableRepeatMode() {
		if (repeatWhileFound) {
			if (foundRecords())
				setRepeat(true);
		}
		if (repeatWhileSubFound) {
			if (subFoundRecords())
				setRepeat(true);
		}
	}

	void performSlaves(Context context) {
		if (!hasIterationSlaves())
			return;

		try {

			for (SlavePerformer s : slaves) {

				if (this.jumpToNextRecord) {
					this.jumpToNextRecord = false;
					return;
				}

				executeSlave(s, context);

			}

			setSlavesAsDone(false);

		} catch (Exception e) {
			if (!treat(e, context)) {
				SavePoint sp = getExceptionCurrentSavePoint(e);
				if (sp != null) {
					getProgram().savePoint = sp;
					sp.save();
				}
//				Util.throwAsRuntimeException(e);
				throw new RuntimeException(e);
			}
		}

	}

	private synchronized void incExecution(int i) {
		execution += i;
	}

	public boolean defaultDataSetNext(DataSet dataSet) {
		boolean n = dataSet.next();
//		if (n) {
//			internalIncRecordCount(dataSet);
//		}
		return n;
	}

	private void internalIncRecordCount(Record record) {
		int inc = 1;

		if (record != null) {
			Field f = record.fieldByName("_affectedrows");
			if (f != null) {
				Integer i = (Integer) record.getValue(f);
				if (i != null)
					inc = i;
			}
		}

		incRecordCount(inc);
	}

	private void setSlavesAsDone(boolean done) {
		for (SlavePerformer p : slaves) {
			p.setAsDone(done);
		}
	}

	private boolean seekForSavePoint(Record record) {
		if (savePointValues != null) {

			if (matchSavePoint(record)) {
				savePointValues = null;
				return false;
			} else
				return true;
		}

		if (isDone()) { // processa todo o dataSet pra chegar ao final
			return true;
		} else
			return false;
	}

	private void registerSavePoint(Record record) {
		if (getProgram().savePoint == null)
			return;

		getProgram().savePoint.removeEntries(getFullName());
		// deve remover inclusive todos os "filhos" (slaves) - para n�o ficar
		// inconsistente
		// ex.: filhos com um estado da itera��o anterior, enquanto a itera��o j� mudou
		if (savePointWrite) {
			getProgram().savePoint.register(this.getFullName(), savePointColumns, record);
			getProgram().savePoint.save();
		}
	}

	private Language language = Language.defaultLanguage();

	private boolean matchSavePoint(Record record) {
		for (String col : savePointValues.keySet()) {
			String v1 = savePointValues.get(col);
			Object rv = record.getValue(col);
			String v2 = language.simpleValue(rv);
			if (!v2.equals(v1))
				return false;
		}
		return true;
	}

	private void showExecutionStatus(Date t) {
		String s;
		if (repeat)
			s = "Execução concluída";
		else
			s = "Execução " + execution + " concluída";

		println(s, " em ", Util.elapsedTimeText(t), ": " + getRecordCount(), " registros processados.");
	}

	private void performDefaultEndImportingData(ArrayList<SlavePerformer> slaves, DataSet dataSet,
			Context invokerContext) {
		for (SlavePerformer p : slaves)
			p.performDefaultEndImportingData(dataSet, invokerContext);
	}

	private void performDefaultStartImportingData(ArrayList<SlavePerformer> slaves, DataSet dataSet,
			Context invokerContext) {
		for (SlavePerformer p : slaves)
			p.performDefaultStartImportingData(dataSet, invokerContext);
	}

	private void dispatchAfterLastEvents(Context context) {

		for (SlavePerformer p : slaves)
			p.triggerAfterLastRowEvent(context);

	}

	private void dispatchBeforeFirstEvents(Context context) {

		for (SlavePerformer p : slaves)
			p.triggerBeforeFirstRowEvent(context);

	}

	protected void showScene(int offset, Record record) {
		if (record == null)
			return;

		String s = "";

		try {
			s = record.toString();
		} catch (Exception e) {
			s = "";
		}

		// if (s.length() > 0)
		// return;
		if (s.length() > 0)
			println("[" + s + "]");
	}

	@Override
	public SourcePerformer nearestSource() {
		return this;
	}

	public void addSlave(Performer slave) {
		if (slave == this)
			throw new RuntimeException("Erro interno");

		slaves.add(slave);
	}

//	@Deprecated
//	private void multiThreadWaitSlaves() {
//		try {
//
//			for (SlavePerformer s : slaves) {
//
//				synchronized (s) {
//					// aguarda conclus�o de todas as threads relacionadas aos slaves
//					if (s.getExecutionThreadId() != null)
//						s.wait();
//				}
//
//			}
//
//		} catch (InterruptedException e) {
//			throw new RuntimeException(e);
//		}
//	}

	void executeSlave(SlavePerformer slave, Context context) {

		Context c = join.suitContext(context);
		Event e = slave.getEvent();

		if ((e == null) || (e.checkEachRow(c)))

			slave.execute(context);

	}

	/**
	 * Indica que existem Slaves que ser�o executados durante o processo de itera��o
	 * do SourceQuery. �til para aprimorar a performance nos casos onde n�o h�.
	 */
	private boolean hasIterationSlaves() {
		if (hasIterationSlaves != null)
			return hasIterationSlaves;

		for (SlavePerformer p : slaves) {
			Event e = p.getEvent();
			if (e == null) {
				hasIterationSlaves = true;
				return true;
			}
			if (e.isIterableOnRows())
				return true;
		}
		return false;
	}

	private static final String letras = "abcdefghijklmnopqrstuvwxyz";

	@Override
	String childId(Performer performer) {
		if (slaves.size() <= 1)
			return "";
		int i = slaves.indexOf(performer);
		if (i == -1) {
			return super.childId(performer);
		}
		char c = letras.charAt(i);
		return c + "";
	}

	private String[] savePointColumns = null;

	public String[] getSavePointColumns() {
		return savePointColumns;
	}

	private Map<String, String> savePointValues = null;

	private boolean savePointWrite = true;

	public boolean isTerminal() {
		return slaves.size() == 0;
	}

	/*
	 * protected static final int FORWARD = +1; protected static final int BACKWARD
	 * = -1; protected static final int EQUAL = 0; protected static final int
	 * UNKNOWN = 0;
	 * 
	 * private int sortDirection = FORWARD; // unknown
	 */
	@Override
	public void assignConfigVar(String var, String value) {

		if (var.equalsIgnoreCase("FOUND_RECORDS_THRESHOLD")) {
			int t = Integer.parseInt(value);
			setFoundRecordsThreshold(t);
			return;
		}

		if (var.equalsIgnoreCase("REPEAT")) {
			boolean v = Boolean.parseBoolean(value);
			setRepeat(v);
			return;
		}

		if (var.equalsIgnoreCase("REPEAT_WHILE_FOUND")) {
			boolean v = Boolean.parseBoolean(value);
			repeatWhileFound = v;
			return;
		}

		if (var.equalsIgnoreCase("REPEAT_WHILE_SUB_FOUND")) {
			boolean v = Boolean.parseBoolean(value);
			repeatWhileSubFound = v;
			return;
		}

		if (var.equalsIgnoreCase("SAVEPOINT")) {
			savePointColumns = Util.splitByComma(value, true);
			getProgram().assignSavePoint(true);
			return;
		}

		if (var.equalsIgnoreCase("SAVEPOINT_WRITE")) {
			boolean v = Boolean.parseBoolean(value);
			savePointWrite = v;
			return;
		}

		if (var.equalsIgnoreCase("OUTPUT_FILE")) {
			throw new RuntimeException("Propriedade OUTPUT_FILE no Source foi descontinuada. Utilize um target.");
		}

		if (var.equalsIgnoreCase("ASYNC_ITERATION")) {
			boolean v = Boolean.parseBoolean(value);
			asyncIteration = v;
			return;
		}

		if (var.equalsIgnoreCase("ASYNC_LOADING")) {
			boolean v = Boolean.parseBoolean(value);
			asyncLoading = v;
			return;
		}

		if (var.equalsIgnoreCase("JOIN")) {
			if (value.equalsIgnoreCase("CARTESIAN")) {
				join = new Cartesian(this);
				return;
			}
			if (value.equalsIgnoreCase("FOLLOWER")) {
				join = new FollowerJoin(this);
				return;
			}
			throw new RuntimeException("Tipo de join desconhecido: " + value);
		}

		if (var.equalsIgnoreCase("JOIN_KEY")) {
			if (!(join instanceof MatchJoin))
				throw new RuntimeException("N�o � permitido associar JOIN_KEY em um source com join do tipo "
						+ join.getClass().getSimpleName());
			MatchJoin j = (MatchJoin) join;
			CommandParser.setKeyDefinition(j, value);
			return;
		}

		super.assignConfigVar(var, value);
	}

//	public String getIterationThreadId() {
//		return iterationThreadId;
//	}
//
//	public void setIterationThreadId(String iterationThreadId) {
//		this.iterationThreadId = iterationThreadId;
//	}

	// protected void checkRecordsForActionsEachRow(DataSet source) throws
	// Exception {
	// boolean found = foundRecords();
	//
	// if (found)
	// executeActionIfRecordsFound(source);
	// }

	@Override
	protected boolean showDataRow() {
		if (showDataRow == null)
			return isTerminal();
		return showDataRow;
	}

	@Override
	public void recursivelyGenerateImplicitSlaves() {
		// gera o Slave correspondente ao OUTPUT_FILE
		// if (outputFileName != null)
		// generateOutputFileSlave();

		for (SlavePerformer p : slaves)
			p.recursivelyGenerateImplicitSlaves();
	}

	@Override
	public boolean repeat() {
		return repeat;
	}

	@Override
	public void setRepeat(boolean repeat) {
		this.repeat = repeat;
	}

	@Override
	public void executeActionIfRecordsFound(Context context) {
		if (actionIfFound == null)
			return;
		actionIfFound.execute(context);
		// dispatchEvent("iffound", source);
	}

	@Override
	public void executeActionIfNoRecordsFound(Context context) {
		if (actionIfNotFound == null)
			return;
		actionIfNotFound.execute(context);
		// dispatchEvent("ifnotfound", source);
	}

	public Join join = new Cartesian(this);

	private boolean jumpToNextRecord = false;

//	public int getSortDirection() {
//		return sortDirection;
//	}

//	public void setSortDirection(int sortDirection) {
//
//		if ((sortDirection != FORWARD) && (sortDirection != BACKWARD))
//			throw new RuntimeException("Sentido inválido");
//		this.sortDirection = sortDirection;
//	}

	@Override
	public void notifyInvokerRequestedDataSet(SourcePerformer invoker, Context context,
			Future<DataSet> loadingDataSet) {
		join.invokerRequestedDataSet(invoker, context, loadingDataSet);
	}

	public void jumpToNextRecord() {
		jumpToNextRecord = true;
	}

	public int indexOfSlave(Performer slave) {
		return slaves.indexOf(slave);
	}

	@Override
	protected boolean showPerformance() {
		if (slaves.size() > 0)
			return false;
		return super.showPerformance();
	}

	@Override
	public void restoreSavePointProperty(String column, String value) {
		if (savePointValues == null)
			savePointValues = new HashMap<String, String>();
		savePointValues.put(column, value);
	}

	@Override
	public Device findDeviceRel(String relName) {
		String[] ss = relName.split("\\.");
		SlavePerformer sl = null;
		for (SlavePerformer p : slaves) {
			if (p.getSimpleName().equals(ss[0]))
				sl = p;
		}

		if (sl == null) {
			Device r = super.findDeviceRel(relName);
			if (r != null)
				return r;
		}

		if (ss.length == 1)
			return sl;

		relName = relName.substring(ss[0].length() + 1);
		if (sl == null)
			throw new RuntimeException("Propriedade n�o encontrada: " + relName);
		return sl.findDeviceRel(relName);
	}

	public Device findDevice(Predicate<Device> criteria) {
		Device r = null;
		if (criteria.test(this))
			r = Assert.notDifferent(r, this);
		for (SlavePerformer p : slaves) {
			Device n = p.findDevice(criteria);
			if (r != null) {
				if (n != null)
					if (r != n)
						throw new RuntimeException("Duplicidade de devices para o mesmo crit�rio");
			} else
				r = n;
		}
		return r;
	}

	@Override
	protected String getTypeName() {
		return "source";
	}

	public void showTree() {
		super.showTree();

		String s = Util.repeat("\t", depth());

		int i = 0;
		for (SlavePerformer p : slaves) {
			System.out.println(s + "slave[" + i + "]:");
			p.showTree();
			i++;
		}
	}

	protected static Integer compare(Object a, Object b) {
		if ((a == null) || (b == null))
			return null;
		/*
		 * if (a == null) { if (b == null) return EQUAL; else return FORWARD; } else if
		 * (b == null) return BACKWARD;
		 */

		if ((a instanceof Comparable) && (b instanceof Comparable))
			return ((Comparable) b).compareTo((Comparable) a);

		throw new RuntimeException("Valores não comparáveis: " + a + " e " + b);
	}

	public ArrayList<SlavePerformer> getSlaves() {
		return slaves;
	}

	public Join getJoinType() {
		return join;
	}

	public DataSet dataSetInUse() {
		return dataSetInUse;
	}

	public void freeDataSetInUse() {
		dataSetInUse = null;
	}

	@Override
	public SavePoint getExceptionCurrentSavePoint(Exception e) {
		if (getProgram().savePoint == null)
			return null;

		if (e == null)
			return getProgram().savePoint;

		SavePoint r = getProgram().savePoint;

		for (SlavePerformer p : slaves) {
			SavePoint c = p.getExceptionCurrentSavePoint(e);
			if (c == null)
				continue;

			if (c.getLastUpdate() < r.getLastUpdate())
				r = c;
		}

		return r;
	}

	private void sendTargetBatches(Context context) {
		for (SlavePerformer p : slaves) {
			if (p instanceof TargetPerformer) {
				((TargetPerformer) p).executeBatch(context);
			}
		}

	}

	@Override
	public void performDefaultStartImportingData(DataSet dataSet, Context context) {
		// nada (somente o Target executa)
	}

	@Override
	public void performDefaultEndImportingData(DataSet dataSet, Context context) {
		// nada (somente o Target executa)
	}

	@Override
	public void notifyInvokerJumpedNext(DataSet dataSet, Context invokerContext) {
		join.newUpperRecord(dataSet, invokerContext);
	}

	@Override
	protected String baseForSimpleName() {
		return "s";
	}

	@Override
	protected void reportCounter() {
		join.reportCounter();
	}

	@Override
	protected void incRecordCount(int inc) {
		join.incRecordCount(inc);
	}

}
