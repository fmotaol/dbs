package core.performer;

import java.io.File;
import java.sql.SQLException;
import java.util.function.Predicate;

import core.DBS;
import core.Device;
import core.Macro;
import core.SavePoint;
import core.dataset.DataSet;
import core.dataset.Record;
import core.jdbc.JDBCConnection;
import core.parsing.CommandParser;
import util.logical.Assert;

public class TargetPerformer extends Performer implements SimplePerformer {

	private Integer batchSize;

	private String tableName;

	private boolean ignoreUnknownFields = false;

	private String outputFileName = null;

	private String appendingFile = null;

	public static int globalAffectedRows = 0;

	public void setAppendingFile(String appendingFile) {
		this.appendingFile = appendingFile;
	}

	public TargetPerformer(Macro engine, String connectionId, Performer invoker) {
		super(engine, connectionId, invoker);
		this.ignoreUnknownFields = getProgram().ignoreUnknownFields;
		
	}

	private void setDefaultImport(String templateSQL) {
		templateSQL = templateSQL.trim();
		if (templateSQL == null)
			return;
		if (isDefaultImport(templateSQL)) {
//			isDefaultImport = true;
			templateSQL = templateSQL.substring("default import".length());
			if (templateSQL.endsWith(";"))
				templateSQL = templateSQL.substring(0, templateSQL.length() - 1);

			tableName = templateSQL.trim();
			if (tableName.isEmpty())
				tableName = null;
		}
	}

	private boolean isDefaultImport(String command) {
		if (command == null)
			return false;
		command = command.trim();
		command = CommandParser.clearCommentedLines(command);
		return command.startsWith("default import");
	}

	@Override
	protected void showScene(int offset, Record bufferedRecord) {
		// nada
	}

	@Override
	public Result perform(String templateSQL, Context context) {

		if (isDone()) {
			setAsDone(false);
			return null;
		}

		delay(delayTimeBefore);

		execution++;

		showStackCurrentScene(0, context);
		showLabel(context);
		
		Result r = executeCommand(templateSQL, context);

		executeActionsBasedOnRecordsFound(context);

		setAsDone();
		registerSavePointAsDone();

		setAsDone(false); // para reinício

		delay(delayTimeAfter);

		return r;
	}

	@Override
	public void setTemplateCommand(String command) {
		super.setTemplateCommand(command);
		setDefaultImport(command);
	}

	@Override
	public void restoreSavePointProperty(String property, String value) {
		if ("$appendingFile".equals(property))
			appendingFile = value;

		super.restoreSavePointProperty(property, value);
	}

	@Override
	protected Result executeCommand(String command, Context context) {
		if (isDefaultImport(command)) {
			if (disableCommands)
				return null;

			Record record = context.record;
			String recordPreview = " -> " + record.bySeparator(";");

			getConnection(context).defaultImportRow(this, context);

			if (showStatus) {
				println("[" + DBS.formatTime() + "] Importado registro " + record.getRowId() + recordPreview);
			}

			return new Result(1);

		} else
			return super.executeCommand(command, context);
	}

	@Override
	public SourcePerformer nearestSource() {
		if (invoker == null)
			return null;
		return invoker.nearestSource();
	}

	@Override
	protected boolean printContextId(Context context) {
		boolean shown = super.printContextId(context);
		String c = null;
		if (invoker != null)
			c = invoker.childId(this);
		if (shown && c != null)
			System.out.print(".");
		System.out.print(c);
		return shown || c != null;
	}

	// public void singleExecSQL(String sql, DataSet source) {
	// super.execute(sql, source);
	// }

	@Override
	protected Result executeConcreteSQL(String sql, Context context) {

		Result r = null;

		if (batchSize == null) {

			return super.executeConcreteSQL(sql, context);

		} else {

			setRecordCount(0);
			totalOperations++;

			addIntoBatch(sql);
			appendConditionalCommandsIntoBatch(context);

			if (showStatus)
				reportCounter();
			if (showPerformance())
				checkForReportPerformance();

			if (isBatchFull())
				r = executeBatch(context);
			else
				r = new Result(0);
		}

		registerStateInSavePoint();
		return r;
	}

	static boolean SAVE_RECORD_COUNT = true;

	static boolean SAVE_FILE_FOR_APPEND = true;

	private void registerStateInSavePoint() {
		if (getProgram().savePoint == null)
			return;

		if (SAVE_RECORD_COUNT) {
			Integer trc = getTotalRecordCount();
			getProgram().savePoint.register(this.getFullName(), "$totalRecordCount", trc.toString());
		}
		if (SAVE_FILE_FOR_APPEND) {
			String af = getAppendingFile();
			if (af != null)
				getProgram().savePoint.register(this.getFullName(), "$appendingFile", af);
		}
		getProgram().savePoint.save();
	}

	@Override
	public void assignConfigVar(String var, String value) {

		if (var.equalsIgnoreCase("BATCH_SIZE")) {

			int size;
			try {
				size = Integer.parseInt(value);
			} catch (Exception e) {
				throw new RuntimeException("BATCH_SIZE deve especificar um valor inteiro");
			}

			if (size <= 1)
				throw new RuntimeException("BATCH_SIZE deve ser maior do que 1");

			setBatchSize(size);

			return;
		}

		if (var.equalsIgnoreCase("DISCARD_UNKNOWN_FIELDS")) {
			warning("Atributo DISCARD_UNKNOWN_FIELDS deve ser renomeado para IGNORE_UNKNOWN_FIELDS");
			boolean v = Boolean.parseBoolean(value);
			ignoreUnknownFields = v;
			return;
		}

		if (var.equalsIgnoreCase("IGNORE_UNKNOWN_FIELDS")) {
			boolean v = Boolean.parseBoolean(value);
			ignoreUnknownFields = v;
			return;
		}

		if (var.equalsIgnoreCase("OUTPUT_FILE")) {
			this.outputFileName = value;
			return;
		}

		super.assignConfigVar(var, value);
	}

	private void setBatchSize(int size) {
		// assignNewBatch();
		batchSize = size;
	}

	@Override
	protected boolean showDataRow() {
		if (showDataRow == null)
			return false;
		return showDataRow;
	}

	@Override
	public void recursivelyGenerateImplicitSlaves() {
		// nada
	}

	public String getConcreteOutputFileName(Context context) {
		boolean gen = false;
		if (outputFileName == null) {
			outputFileName = generateOutputFileName();
			gen = true;
		}

		String s = outputFileName;

		if (!gen)
			s = defaultConcretizer.concretizeAll(s, context);
		return s;
	}

	private String generateOutputFileName() {
		String name = getProgram().getDBSFileName();

//		if (name.endsWith(".sql") || name.endsWith(".dbs"))
//			name = name.substring(0, name.length() - 4);

		int seq = 0;
		File f;
		do {
			seq++;
			name = name + seq + ".csv";
			f = new File(name);
		} while (f.exists());

		return name;
	}

	@Override
	public boolean repeat() {
		if (invoker != null)
			return invoker.repeat();
		return false;
	}

	@Override
	public void setRepeat(boolean repeat) {
		if (invoker != null)
			invoker.setRepeat(repeat);
		else
			throw new RuntimeException("Impossível atribuir repetição. Nenhum source invocou este target.");
	}

	// @Override
	// public void defaultImportRow(DataSet source, Record record) throws
	// Exception {
	// if (!isDefaultImport)
	// return;
	// DBSConnection c = getConnection();
	// c.defaultImportRow(this, source, record, currentBatch);
	// }

	@Override
	public void performDefaultStartImportingData(DataSet dataSet, Context context) {
		if (!isDefaultImport(context))
			return;

		DBSConnection c = getConnection(context);
		c.defaultStartImportingData(this);
	}

	private boolean isDefaultImport(Context context) {
		return isDefaultImport(getTemplateCommand(context));
	}

	@Override
	public void performDefaultEndImportingData(DataSet dataSet, Context context) {
		if (!isDefaultImport(context))
			return;

		DBSConnection c = getConnection(context);
		c.defaultEndImportingData(this);
	}

	@Override
	public String[] getFieldsToFilter(Context context) {
		if (ignoreUnknownFields) {
			JDBCConnection c = getJDBCConnection();
			// if (c.isInsertOrUpdateCommand(getTemplateCommand()))
			if (hasTableName(context))
				return c.getTableFieldsByTableName(getTableName(context));

		}
		return null;

	}

	private boolean hasTableName(Context context) {
		if (tableName != null)
			return true;

		String templateCommand = getTemplateCommand(context);
		if (getJDBCConnection().isInsertOrUpdateCommand(templateCommand))
			return true;

		return false;
	}

	public String getTableName(Context context) {
		JDBCConnection c = getJDBCConnection();
		if (tableName == null) {
			tableName = c.inferTableName(getTemplateCommand(context));
		}
		return tableName;
	}

	public String[] getTableFields(Context context) {
		try {
			JDBCConnection c = getJDBCConnection();
			String t = getTableName(context);
			return c.getTableFields(t);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public String[] getPrimaryKeyFields(Context context) {
		try {
			JDBCConnection c = getJDBCConnection();
			String t = getTableName(context);
			return c.getPrimaryKeyFields(t);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void reportCounter() {
		boolean ommitZeroRecords = currentBatch != null;
		reportCounter(true, ommitZeroRecords);
	}

	@Override
	protected String getTypeName() {
		return "target";
	}

	public String getAppendingFile() {
		return appendingFile;
	}

	@Override
	public void setRecordCount(int records) {
		globalAffectedRows += records;
		super.setRecordCount(records);
	}

	public Device findDevice(Predicate<Device> criteria) {
		Device r = null;
		if (criteria.test(this))
			r = Assert.notDifferent(r, this);
		return r;
	}

	@Override
	public String[] getSavePointColumns() {
		return null;
	}

	private Batch currentBatch;

	public Result executeBatch(Context context) {
		if (currentBatch == null)
			return null;

		if (disableCommands)
			return null;

		if (showStatus)
			println();

		int affectedRows = 0;

		println("Enviando lote (", currentBatch.size() + " comandos)");
		do {
			tryAgain = false;

			try {

				DBSConnection conn = getConnection(context);
				logSQL(conn, currentBatch.getBuffer().toString());
				Result ar = currentBatch.execute(conn);
				setRecordCount(ar.getAffectedRows());
				logStatusSQLResult(conn, ar);

			} catch (Exception e) {
				if (!treat(e, context))
					throw new RuntimeException(e);
				else
					System.out.println("Erro: " + e.getMessage());

			}

		} while (tryAgain);

		if (getProgram().savePoint != null)
			currentBatch.setSavePoint(getProgram().savePoint.copyState());

		currentBatch.close();
		currentBatch = null;
		assignNewBatch();

		if (showStatus)
			reportCounter();

		return new Result(affectedRows);

	}

	private void addIntoBatch(String sql) {
		assertBatchIsPrepared();
		currentBatch.add(sql, this);
		// show("Adicionado ao lote (", currentBatch.size() + "): ", sql);
	}

	private void assertBatchIsPrepared() {
		if (batchSize != null)
			assignNewBatch();
	}

	private boolean isBatchFull() {
		return currentBatch.size() >= batchSize;
	}

	private void appendConditionalCommandsIntoBatch(Context context) {

		if ((actionIfFound != null) ||

				(actionIfNotFound != null) || (actionByError.size() > 0))

			// batch.appendConditionalAction(sql, aif, anf, ae);

			throw new RuntimeException("Uso de batch não é permitido com cláusulas condicionais");
	}

	@Override
	public SavePoint getExceptionCurrentSavePoint(Exception e) {
		if (currentBatch == null)
			return null;

		return currentBatch.getSavePoint();
	}

	private void assignNewBatch() {
		if (isDynamicConnection())
			throw new RuntimeException("Uso de comandos em lote (batch) não permitido para conexões dinâmicas");

		if (currentBatch == null)
			currentBatch = getConnection().createBatch(this);
	}

	@Override
	protected void executeActionsBasedOnRecordsFound(Context invokerContext) {
		if (currentBatch != null)
			return;
		super.executeActionsBasedOnRecordsFound(invokerContext);
	}

	@Override
	public void notifyInvokerJumpedNext(DataSet dataSet, Context invokerContext) {
		// nada
	}

	@Override
	public void showStackCurrentScene(int offset, Context context) {
		if (showScene)
			super.showStackCurrentScene(offset, context);
	}

	@Override
	protected String baseForSimpleName() {
		return "t";
	}

}
