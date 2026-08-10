package core.performer;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import core.DBS;
import core.Device;
import core.Macro;
import core.args.Argument;
import core.args.UndefinedArgAction;
import core.dataset.DataSet;
import core.dataset.Record;
import core.events.Event;
import core.jdbc.JDBCConnection;
import core.parsing.CommandParser;
import core.parsing.Parse;
import core.parsing.expression.Expression;
import core.parsing.replace.StringConcretizer;
import core.savepoint.SavePoint;
import core.savepoint.SavePointRestoreable;
import core.sql.DefaultLanguage;
import core.sql.Language;
import core.util.Logger;
import util.Ready;
import util.Strings;
import util.Util;
import util.logical.Check;

public abstract class Performer implements SlavePerformer, SavePointRestoreable {

	public static boolean DEFAULT_SHOW_SQL = true;
	public static boolean DEFAULT_SHOW_SCENE = true;
	public static boolean DEFAULT_SHOW_STATUS = true;

	public static boolean DEFAULT_LOG_SQL = false;
	public static boolean DEFAULT_LOG_SCENE = false;
	public static boolean DEFAULT_LOG_STATUS = false;
	public static boolean DISABLE_COMMANDS = false;

	protected boolean showSQL = DEFAULT_SHOW_SQL;
	protected boolean showScene = DEFAULT_SHOW_SCENE;
	public boolean showStatus = DEFAULT_SHOW_STATUS;

	protected boolean logSQL = DEFAULT_LOG_SQL;
	protected boolean logScene = DEFAULT_LOG_SCENE;
	protected boolean logStatus = DEFAULT_LOG_STATUS;

//	protected Logger logger;

	protected Macro engine;

	protected Performer invoker;

	private String templateConnectionId;

	protected StringConcretizer sqlConcretizer;
	public StringConcretizer defaultConcretizer;

	protected int execution = 0;

	protected boolean firstRowIsHeader = true;

	private boolean clearUnknownVarReferences = false;

	private String alias;

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
		if (alias != null && !Strings.containsAny(alias, ".", " ", ","))
			setSimpleName(alias);
		else
			generateSimpleName();
	}

	public boolean firstRowIsHeader() {
		return firstRowIsHeader;
	}

	public void setFirstRowIsHeader(boolean firstRowIsHeader) {
		this.firstRowIsHeader = firstRowIsHeader;
	}

	public String getColumnSeparator() {
		return columnSeparator;
	}

	public void setColumnSeparator(String columnSeparator) {
		this.columnSeparator = columnSeparator;
	}

	public String getRowSeparator() {
		return rowSeparator;
	}

	public void setRowSeparator(String rowSeparator) {
		this.rowSeparator = rowSeparator;
	}

	protected String columnSeparator = ";";

	protected String rowSeparator = "\r\n";

	protected String decimalSeparator;
	protected String dateFormat = null;

	private int recordCount;

	private int totalRecordCount = 0;

	public void generateSimpleName() {
		String b = baseForSimpleName();
		for (int i = 1; i < 10000; i++) {
			String s = b + i;
			DBS program = getProgram();
			if (program.findPerformerBySimpleName(s) == null) {
				this.setSimpleName(s);
				program.allNamedPerformers.add(this);
				return;
			}
		}

		throw new RuntimeException("Esgotado o limite de rotinas");
	}

	protected abstract String baseForSimpleName();

	public Performer(Macro engine, String templateConnectionId, Performer invoker) {
		super();
		if (invoker == this)
			throw new RuntimeException("Erro interno");

		defaultConcretizer = new StringConcretizer(engine, this, false);

		this.engine = engine;
		this.invoker = invoker;

		sqlConcretizer = new StringConcretizer(engine, this, true);

		this.templateConnectionId = templateConnectionId;

		sqlConcretizer.setRecursiveReference(getProgram().recursiveReference);

		generateSimpleName();
	}

	protected TargetPerformer actionIfNotFound;
	protected TargetPerformer actionIfFound;

	private String simpleName;

	public void setSimpleName(String simpleName) {
		this.simpleName = simpleName;
	}

	public enum ActionType {
		EXECUTE, GENERATE_SQL
	};

	protected ActionType actionType = ActionType.EXECUTE;

	public TargetPerformer getActionIfNotFound() {
		return actionIfNotFound;
	}

	public void setActionIfNotFound(TargetPerformer actionIfNotFound, Integer threshold) {
		this.actionIfNotFound = actionIfNotFound;
		if (threshold != null)
			setFoundRecordsThreshold(threshold);
		actionIfNotFound.setSimpleName("ifnotfound");
	}

	public TargetPerformer getActionIfFound() {
		return actionIfFound;
	}

	public void setActionIfFound(TargetPerformer actionIfFound, Integer threshold) {
		this.actionIfFound = actionIfFound;
		if (threshold != null)
			setFoundRecordsThreshold(threshold);
		actionIfFound.setSimpleName("iffound");
	}

	protected Map<String, SimplePerformer> actionByError = new HashMap<String, SimplePerformer>();

//	protected String prepareStatementName = null;

	protected boolean disableCommands = DISABLE_COMMANDS;

	protected boolean tryAgain = false;

	protected abstract Result perform(String templateSQL, Context context);

	void executeSetVarAction(String action, Context context, boolean perform) {
		action = action.substring(4);
		String[] ss = action.split(";");
		for (String s : ss) {
			setVarValue(s, context, perform);
		}
	}

	private void setVarValue(String exp, Context context, boolean perform) {
		String[] ss = exp.split("=", 2);
		if (ss.length != 2)
			throw new RuntimeException("Erro de sintaxe em 'set <var> = <value>'");

		String name = ss[0].trim();
		String value = ss[1].trim();

//		Record record = null;
//		if (dataSet != null)
//			record = dataSet.currentRecord();

		value = sqlConcretizer.concretizeAll(value, context);

		if (perform) {
			engine.writeVar(name, value);
		}
	}

	void executeAppendVarAction(String action, Context context, boolean perform) {
		action = action.substring(7);
		String[] ss = action.split("=", 2);
		if (ss.length != 2)
			throw new RuntimeException("Erro de sintaxe em \"append <var> = <value>\"");

		String name = ss[0].trim();
		StringConcretizer.checkVarNameSyntax(name);
		String value = ss[1].trim();

		value = defaultConcretizer.concretizeAll(value, context);

		String s = engine.readVar(name);
		if (perform) {
			engine.writeVar(name, s + value);
		}
	}

	void executeClearVarAction(String cmd, boolean perform) {
		// cmd = cmd.substring(4);
		// if (cmd.endsWith(";"))
		cmd = cmd.substring(0, cmd.length() - 1);
		StringConcretizer.checkVarNameSyntax(cmd);

		if (perform) {
			engine.clearVar(cmd);
		}
	}

	void executeSaveRecordAction(String action, Context context, boolean perform) {
		String[] ss = action.split("save record as");
		if (ss.length != 2)
			throw new RuntimeException("Erro de sintaxe em 'save record as'");
		String recordName = ss[1].trim();
		// if (recordName.endsWith(";"))
		// recordName = recordName.substring(0, recordName.length() - 1);
		StringConcretizer.checkVarNameSyntax(recordName);
		if (perform) {
			context.saveCurrentRecord(recordName);
		}
	}

	protected Result executeCommand(String command, Context context) {

		if (!(getConnection(context) instanceof DefaultConnection))
			if (DefaultConnection.parseDBSAction(this, command, !disableCommands, context))
				return null;

		if (getProgram().usePreparedStatements)
			throw new RuntimeException("ainda não implementado");
		// TODO implementar

		return executeTemplate(command, context);
	}

	protected void updateDynamicConnection(DataSet source, Record invokerRecord) {
		if (!isDynamicConnection())
			return;
	}

	boolean isDynamicConnection() {
		if (templateConnectionId == null)
			return false;
		String t = templateConnectionId;
		t = t.replace("@arg[", "");
		return t.contains("@");
	}

	public Result executeTemplate(String templateSQL, Context context) {

		Date t = new Date();

		String concreteSQL = concretizeAll(templateSQL, context);
		if (concreteSQL == null)
			concreteSQL = "";

		if (showStatus) {
//			show();
			String cid = getConcreteConnectionId(context);
			if (cid != null) {
				showContextId(context, "Enviando comando (", cid, "):");
			}
		}

		if (showSQL && !concreteSQL.isEmpty())
			println(concreteSQL);

		Result r = null;

		if (showStatus && !"".equals(concreteSQL))
			println("[" + DBS.formatTime() + "] Aguardando resultado...");
//				getWaiter().start();

		r = executeConcreteSQL(concreteSQL, context);

		if (showStatus /* && !"".equals(concreteSQL) */)
			println("[" + DBS.formatTime() + "] " + r.getAffectedRows() + " registro(s) afetado(s)");

//			} finally {
//				getWaiter().end();
//			}

		if (showStatus /* && !"".equals(concreteSQL) */)
//			if (connectionId != null)
			println("Concluida operacao em ", Util.elapsedTimeText(t));

		return r;

	}

	private String concretizeAll(String templateSQL, Context context) {
		if (Util.nullOrEmpty(templateSQL))
			return templateSQL;

		StringConcretizer c = getSuitableConcretizer(context);
		return c.concretizeAll(templateSQL, context);
	}

	public String getConcreteConnectionId(Context context) {
		String r = defaultConcretizer.concretizeAll(templateConnectionId, context);
		return r;
	}

	ArrayList<Exception> exceptionLocks = new ArrayList<Exception>();

	protected boolean treat(Exception exception, Context context) {
		if (locked(exception))
			return false;

		SimplePerformer action = findBestAction(exception);

		if (action == null)
			return false;

//		show("Executando ação de contorno - erro do tipo ", exception.getClass().getSimpleName(), ": ",
//				Util.abrev(exception.getMessage(), 50));

		lockExceptionTreatment(exception);
		context.setException(exception);

		try {

			Result rs = action.perform(templateCommand.getCommand(context), context);
			return rs != null;

		} finally {

			context.setException(null);
			unlockExceptionTreatment(exception);

		}

	}

	private SimplePerformer findBestAction(Exception exception) {
		if (exception == null)
			return null;

		SimplePerformer a = null;
		if (exception instanceof SQLException) {
			a = findActionBySQLErrorCode((SQLException) exception);
			if (a != null)
				return a;
		}

		Class<? extends Exception> clazz = exception.getClass();
		a = recursiveFindActionByExceptionClass(clazz);
		if (a != null)
			return a;

		a = findActionByError(exception.getMessage());
		if (a != null)
			return a;

		a = findActionByMessagePattern(exception.getMessage());
		if (a != null)
			return a;

		if (exception instanceof SQLException) {
			SQLException e = ((SQLException) exception).getNextException();
			if (e != null) {
				a = findBestAction(e);

				if (a != null)
					return a;
			}
		}

		Throwable e = exception.getCause();
		if (e != null) {
			if (e instanceof Exception) {
				a = findBestAction((Exception) e);

				if (a != null)
					return a;
			}
		}

		return a;
	}

	private SimplePerformer findActionByMessagePattern(String message) {
		for (String s : actionByError.keySet()) {
			if (message.matches(s)) {
				SimplePerformer a = actionByError.get(s);
				return a;
			}
		}
		return null;
	}

	private SimplePerformer recursiveFindActionByExceptionClass(Class<? extends Throwable> clazz) {
		SimplePerformer a = findActionByExceptionClass(clazz);
		if (a != null)
			return a;

		Class<? extends Throwable> parent = (Class<? extends Throwable>) clazz.getSuperclass();
		if (parent == null)
			return null;

		if (parent == Throwable.class)
			return null;

		return recursiveFindActionByExceptionClass(parent);
	}

	private SimplePerformer findActionByExceptionClass(Class<? extends Throwable> clazz) {
		SimplePerformer a = findActionByError(clazz.getSimpleName());
		if (a != null)
			return a;

		a = findActionByError(clazz.getName());
		if (a != null)
			return a;

		a = findActionByError(clazz.getCanonicalName());
		if (a != null)
			return a;

		// if (a != null)
		// return a;

		return null;
	}

	private SimplePerformer findActionBySQLErrorCode(SQLException exception) {
		SQLException se = (SQLException) exception;
		int errorCode = se.getErrorCode();
		String sec = errorCode + "";

		return findActionByError(sec);
	}

	private SimplePerformer findActionByError(String error) {
		return actionByError.get(error);
	}

	private void unlockExceptionTreatment(Exception e) {
		exceptionLocks.remove(e);
	}

	private void lockExceptionTreatment(Exception e) {
		exceptionLocks.add(e);
	}

	private boolean locked(Exception e) {
		return exceptionLocks.contains(e);
	}

	protected int promptAndIndent() {

		String s = getPromptText();
		int r = s.length();
		System.out.print(s);

//		int depth = depth() + offset;
//		int depth = offset + 1;
//		engine.print(depth, (String) null);
		return r;
	}

	private String getPromptText() {
		String promptExp;
		promptExp = getFullPath(PathField.ALIAS, "\\");

		if (!promptExp.startsWith(engine.getName()))
			promptExp += "/" + engine.getName();
		String s = promptExp + "> ";
		return s;
	}

//	public void show(String... s) {
//		show(0, s);
//	}

//	private void printSpacesLikePrompt() {
//		int l = engine.getName().length();
//		for (int i = 0; i < l; i++)
//			Macro.print(" ");
//	}

	public void println(final String... ss) {

		int pl = promptAndIndent();
		String spaces = Strings.repeat(" ", pl);

//		boolean lineBreak = false;
		for (String t : ss) {
			t = t.replace("\\n", spaces + "\\n");
			DBS.print(t);
//			lineBreak = t.endsWith("\\n");
		}

//		if (!lineBreak)
		Macro.println();
	}

	boolean isQuery(String sql) {
		sql = sql.trim();
		sql = sql.toLowerCase();
		return sql.startsWith("select");
	}

	protected Result executeConcreteSQL(String concreteSQL, Context context) {

		if (disableCommands)
			return new Result(0);

		Result er = null;

		do {
			tryAgain = false;

			try {

				DBSConnection con = getConnection(context);
				logSQL(con, concreteSQL);			
				er = con.execute(concreteSQL, this, context);
				logStatusSQLResult(con, er);
								
				totalOperations++;
				setRecordCount(er.getAffectedRows());

			} catch (Exception e) {
				if (treat(e, context))
					return er;

				do {
					if (e instanceof SQLException) {
//						e.printStackTrace();
						Exception n = ((SQLException) e).getNextException();
						if (n == null)
							Util.throwAsRuntimeException(e);
						else
							e = n;
					} else
						Util.throwAsRuntimeException(e);

				} while (e != null);

			}

		} while (tryAgain);

		if (showStatus)
			reportCounter();
		if (showPerformance())
			checkForReportPerformance();

		return er;

	}

	void logSQL(DBSConnection con, String concreteSQL) {
		if (!logSQL)
			return;
		getLogger().log(" [" + con.getId() + "] ", concreteSQL);
	}

	void logStatusSQLResult(DBSConnection con, Result res) {
		if (!logStatus)
			return;
		getLogger().log("Connection " + con.getId() + " - Enviando comando:\n", res.toString());
	}

	void logStatusSQLResult(DBSConnection con, DataSet dataSet) {
		if (!logStatus)
			return;
		getLogger().log("Dataset retornado");
	}

	protected void reportCounter() {
		reportCounter(true, false);
	}

	protected void reportAffectedRecords_Old(boolean ommitZeroRecords) {
		if (recordCount > 0) {

			if (recordCount > 1)
				println("-> " + recordCount, " registros afetados.");
			else
				println("-> 1 registro afetado.");
		} else {
			if (!ommitZeroRecords)
				println("-> Nenhum registro afetado.");
		}
		showTotalRecords();

	}

	protected void reportCounter(boolean iteration, boolean ommitZeroRecords) {
		String msg = "";
		if (recordCount > 0) {

			if (recordCount > 1)
				msg = recordCount + " registros afetados.";
			else
				msg = "1 registro afetado.";
		} else {
			if (!ommitZeroRecords)
				msg = "Nenhum registro afetado.";
		}
		if (iteration)
			msg = "Execução " + execution + ". " + msg;

		println("-> " + msg);
		showTotalRecords();

	}

	protected void checkForReportPerformance() {

		if (showPerformance()) {
			if (totalOperations == (totalOperations / 100) * 100) {
				reportPerformance();
			}
		}

	}

	protected boolean showPerformance() {
		if (showPerformance != null)
			return showPerformance;

		return getEvent() == null;
	}

	private void showTotalRecords() {
		if (totalRecordCount != 0) {
			if (totalRecordCount != 1) {
				println("-> " + totalRecordCount, " registros afetados no total.");
			} else {
				println("-> 1 registro afetado no total.");
			}
		}
	}

	public int geRecordCount() {
		return recordCount;
	}

	public void setRecordCount(int records) {
		this.recordCount = records;
		this.totalRecordCount += records;
	}

//	public void showStackCurrentScene(int offset, Context context) {
//		Record record = null;
//		if (context != null)
//			record = context.record;
//
//		if (invoker != null) {
//			if (context != null)
//				context = context.parent;
//			invoker.showStackCurrentScene(offset, context);
//		} else {
//			showArgsScene(offset);
//		}
//
//		if (showScene)
//			showCurrentScene(offset, record);
//	}

	public void showStackCurrentScene(int offset, Context context) {
		if (invoker != null) {
			if (context != null)
				context = context.getParent();
			invoker.showStackCurrentScene(offset, context);
		} else {
			showArgsScene(offset);
		}

		if (showScene)
			showScene(offset, context);
	}

	protected void showScene(int offset, Context context) {
		if (context == null)
			return;
		showScene(offset, context.record);
	}

	protected abstract void showScene(int offset, Record record);

//	public DataSet query(String sql) throws SQLException, IOException, ParseException {
//		DataSet r = query(connectionId, sql, this);
//		return r;
//	}

	protected Object queryAsSingleValue(String connectionId, String sql)
			throws SQLException, IOException, ParseException {
		DataSet rs = query(connectionId, sql, this);
		if (rs.next()) {
			Object r = rs.currentRecord().getValue(1);
			if (rs.next())
				throw new RuntimeException("Consulta retorna mais de um registro");
			return r;
		}
		rs.close();
		return null;
	}

	public Record queryAsSingleRow(String connectionId, String sql) throws SQLException, IOException, ParseException {
		DataSet ds = query(connectionId, sql, this);
		Record r = null;
			while (ds.next()) {
			if (r != null)
				throw new RuntimeException("Consulta retorna mais de um registro");
			r = ds.currentRecord().createBuffer();
		}
		ds.close();
		return r;
	}

	protected DataSet query(String connectionId, String sql, Performer invoker) {

		DBSConnection conn = getProgram().getConnection(connectionId);

		println("[" + DBS.formatTime() + "] Consultando dados (" + connectionId + "):");
		Date t = new Date();

		if (showSQL)
			println(sql);

		println("[" + DBS.formatTime() + "] Aguardando resultado...");

		logSQL(conn, sql);
		DataSet r = conn.query(sql, this);
		logStatusSQLResult(conn, r);

		reportTimeResult(t);
		return r;
	}

	public DataSet subquery(String connId, String sql, Performer invoker) {
		DataSet r = query(connId, sql, invoker);
		setPreviousSubqueryResult(r);
		return r;
	}

	public void setPreviousSubqueryResult(DataSet r) {
		previousSubqueryResult = r;
	}

	private void reportTimeResult(Date t) {
		println("Retornado em ", Util.elapsedTimeText(t));
//		DBS.println();
	}

//	protected void showContextId(Record record, String... nullForId) {
//		promptAndIndent(0);
//		printContextId(record, nullForId);
//		System.out.println();
//	}

	protected void showContextId(Context context, String... nullForId) {
		promptAndIndent();
		printContextId(context, nullForId);
		System.out.println();
	}

	protected void printContextId(Context context, String... nullForId) {
		for (int i = 0; i < nullForId.length; i++) {
			String s = nullForId[i];
			if (s == null)
				printContextId(context);
			else
				System.out.print(s);
		}
	}

	protected boolean printContextId(Context context) {
		if (context == null)
			return false;
		return context.printContextIdArray();
	}

	// public String getActionIfError() {
	// return actionIfError;
	// }

	public void setActionIfError(TargetPerformer actionIfError, String error) {
		if (error.contains(",")) {
			String[] ss = error.split(",");
			for (String e : ss) {
				String s = e.trim();
				setActionIfError(actionIfError, s);
			}
			return;
		}
		if (actionByError.containsKey(error))
			throw new RuntimeException("Declaração #iferror duplicada");

		actionByError.put(error, actionIfError);

	}

	public void executeActionIfRecordsFound(Context invokerContext) {
		if (actionIfFound == null)
			return;

		actionIfFound.execute(invokerContext);

	}

	public void executeActionIfNoRecordsFound(Context invokerContext) {
		if (actionIfNotFound == null)
			return;

		actionIfNotFound.execute(invokerContext);
	}

	public boolean foundRecords() {
		int threshold = 1;
		if (foundRecordsThreshold != null)
			threshold = foundRecordsThreshold;

		boolean found = recordCount >= threshold;
		return found;
	}

	public int depth() {
		if (invoker == null)
			return 1;
		return invoker.depth() + 1;
	}

	public abstract SourcePerformer nearestSource();

	private Event event;

	// private String eventType = null;

	// private String eventParameters = null;

	private ArrayList<String> labels = new ArrayList<String>();

	private Boolean autoCommit;

//	@Deprecated
//	private String templateCommand_Old;

	private TemplateCommand templateCommand = new NoCommand(this);

	protected Boolean showDataRow = null;

	public Performer getInvoker() {
		return invoker;
	}

	public boolean showSQL() {
		return showSQL;
	}

	public void setShowSQL(boolean showSQL) {
		this.showSQL = showSQL;
	}

	public String subqueryRowSeparator = ", ";

	public String subqueryColumnSeparator = ", ";
	private List<Expression<Boolean>> conditions = new ArrayList<>();

	public void assignConfigVar(String var, String value) {

		if (var.equalsIgnoreCase("LABEL") || var.equalsIgnoreCase("PRINT")) {
			labels.add(value);
			return;
		}

		if (var.equalsIgnoreCase("CONDITION")) {
			Expression<Boolean> exp;
			try {
				exp = CommandParser.parseCondition(value);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			conditions.add(exp);
			return;
		}

		if (var.equalsIgnoreCase("EVENT")) {
			String[] ss = value.split(":");
			if (ss.length > 2)
				throw new RuntimeException("Especificação de evento incorreta");

			String eventType = ss[0].trim();
			String params = null;
			if (ss.length > 1)
				params = ss[1].trim();
			assignEvent(eventType, params);
			return;
		}

		// TODO avaliar se isto não deveria ser uma propriedade da Conexão
		if (var.equalsIgnoreCase("AUTOCOMMIT")) {
			boolean v = Boolean.parseBoolean(value);
			try {
				setAutoCommit(v);
			} catch (IOException e) {
				Util.throwAsRuntimeException(e);
			}
			return;
		}

		if (var.equalsIgnoreCase("ALIAS")) {
			if (!Parse.isSQLIdentifier(value))
				throw new RuntimeException(value + " não é um alias válido");
			setAlias(value);
			return;
		}

		if (var.equalsIgnoreCase("THREAD") || var.equalsIgnoreCase("EXECUTION_THREAD")) {
			throw new RuntimeException("Atributos THREAD e EXECUTION_THREAD descontinuados");
//			if ((value == null) || (value.isEmpty()))
//				throw new RuntimeException("Thread não especificada");
//
//			executionThreadId = value;
//			return;
		}
		if (var.equalsIgnoreCase("DISABLE_COMMANDS")) {
			boolean v = Boolean.parseBoolean(value);
			disableCommands = v;
			return;
		}

		if (var.equalsIgnoreCase("RECURSIVE_REFERENCE")) {
			boolean v = Boolean.parseBoolean(value);
			sqlConcretizer.setRecursiveReference(v);
			defaultConcretizer.setRecursiveReference(v);
			return;
		}

		if (var.equalsIgnoreCase("SHOW_SCENE")) {
			boolean v = Boolean.parseBoolean(value);
			showScene = v;
			return;
		}

		if (var.equalsIgnoreCase("SHOW_STATUS")) {
			boolean v = Boolean.parseBoolean(value);
			showStatus = v;
			return;
		}

		if (var.equalsIgnoreCase("SHOW_SQL")) {
			boolean v = Boolean.parseBoolean(value);
			showSQL = v;
			return;
		}

		if (var.equalsIgnoreCase("LOG_SCENE")) {
			boolean v = Boolean.parseBoolean(value);
			logScene = v;
			return;
		}

		if (var.equalsIgnoreCase("LOG_STATUS")) {
			boolean v = Boolean.parseBoolean(value);
			logStatus = v;
			return;
		}

		if (var.equalsIgnoreCase("LOG_SQL")) {
			boolean v = Boolean.parseBoolean(value);
			logSQL = v;
			return;
		}

		if (var.equalsIgnoreCase("SHOW_DATA_ROW")) {
			boolean v = Boolean.parseBoolean(value);
			showDataRow = v;
			return;
		}

		if (var.equalsIgnoreCase("SHOW_PERFORMANCE")) {
			boolean v = Boolean.parseBoolean(value);
			showPerformance = v;
			return;
		}

		if (var.equalsIgnoreCase("INCLUDE_HEADER")) {
			boolean v = Boolean.parseBoolean(value);
			firstRowIsHeader = v;
			return;
		}

		if (var.equalsIgnoreCase("COLUMN_SEPARATOR")) {
			value = parseAndTreatSystemChars(value);
			columnSeparator = value;
			return;
		}

		if (var.equalsIgnoreCase("LINE_SEPARATOR") || var.equalsIgnoreCase("ROW_SEPARATOR")) {
			value = parseAndTreatSystemChars(value);
			rowSeparator = value;
			return;
		}

		if (var.equalsIgnoreCase("SUBQUERY_COLUMN_SEPARATOR")) {
			value = parseAndTreatSystemChars(value);
			subqueryColumnSeparator = value;
			return;
		}

		if (var.equalsIgnoreCase("SUBQUERY_ROW_SEPARATOR")) {
			value = parseAndTreatSystemChars(value);
			subqueryRowSeparator = value;
			return;
		}

		if (var.equalsIgnoreCase("DECIMAL_SEPARATOR")) {
			value = parseAndTreatSystemChars(value);
			decimalSeparator = value;
			return;
		}

		if (var.equalsIgnoreCase("DELAY_TIME_BEFORE")) {
			value = parseAndTreatSystemChars(value);
			delayTimeBefore = Long.parseLong(value);
			return;
		}

		if (var.equalsIgnoreCase("DELAY_TIME_AFTER")) {
			value = parseAndTreatSystemChars(value);
			delayTimeAfter = Long.parseLong(value);
			return;
		}

		if (var.equalsIgnoreCase("CLEAR_UNKNOWN_VAR_REFERENCES")) {
			value = parseAndTreatSystemChars(value);
			setClearUnknownVarReferences(Boolean.parseBoolean(value));
			return;
		}

		if (var.equalsIgnoreCase("PARSE_CONTROL_CHARS")) {
			boolean v = Boolean.parseBoolean(value);
			sqlConcretizer.parseControlChars = v;
			return;
		}

		if (var.equalsIgnoreCase("IGNORE_ERRORS")) {
			assignIgnoreErrors(value);
			return;
		}

//		if (var.equalsIgnoreCase("DATATYPE_LANGUAGE")) {
//			assignDataTypeLanguage(value);
//			return;
//		}

		throw new RuntimeException("Variável não reconhecida: " + var);
	}

//	@Deprecated
//	private void assignDataTypeLanguage(String value) {
//		Language l = createDataTypeLanguage(value);
//		sqlConcretizer.setLanguage(l);
//		defaultConcretizer.setLanguage(l);
//	}

	private Language createDataTypeLanguage(String value) {
		if (value.equalsIgnoreCase("DefaultLanguage"))
			return new DefaultLanguage();
//		if (value.equalsIgnoreCase("JsonLanguageExt"))
//			return new JsonLanguageExt();

		throw new RuntimeException("DATATYPE_LANGUAGE desconhecido: " + value);
	}

	private void assignIgnoreErrors(String value) {
		if ("false".equalsIgnoreCase(value))
			return;
		if ("".equalsIgnoreCase(value))
			return;
		if ("null".equalsIgnoreCase(value))
			return;

		if ("true".equalsIgnoreCase(value)) {
			actionByError.put("java.lang.Throwable", new EmptyPerformer());
			return;
		}

		String[] ss = Util.splitByComma(value, true);
		for (String s : ss)
			actionByError.put(s, new EmptyPerformer());
	}

	protected String parseAndTreatSystemChars(String value) {
		if (value.contains("\\")) {
			value.replace("\\t", "\t");
			value.replace("\\n", "\n");
			value.replace("\\r", "\r");
			value.replace("\\f", "\f");
			value.replace("\\b", "\b");
			value.replace("\\s", " ");
		}
		return value;
	}

	private void setAutoCommit(boolean autoCommit) throws IOException {
		this.autoCommit = autoCommit;
		try {
			if (isDynamicConnection())
				throw new RuntimeException("Propriedade autocommit não permitida para conexões dinâmicas");

			getConnection().setAutoCommit(autoCommit);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void warning(String... ss) {
		println("AVISO:");
		println(ss);
	}

	public void assignEvent(String eventType, String params) {
		if (event != null)
			throw new RuntimeException("Já existe um evento associado a este performer");

		Event e = Event.create(eventType, (SourcePerformer) getInvoker());

		if (params != null)
			e.setParams(params);

		event = e;
	}

	// public String getEventType() {
	// return eventType;
	// }

	// @Deprecated
	// public void setEventType(String event) {
	// this.eventType = event;
	// if (event != null)
	// if (event.equals("newvalue"))
	// passedValues = new HashSet<String>();
	// }

//	public boolean matchesEvent(Class<Event> eventType) {
//		if (event == null)
//			return false;
//		return event instanceof eventType;
//	}

	// public String getEventParameters() {
	// return eventParameters;
	// }
	//
	// public void setEventParameters(String eventParameters) {
	// this.eventParameters = eventParameters;
	// }

	public boolean checkSQLCondition(String expression, boolean concretize, Context context)
			throws SQLException, IOException, ParseException {
		String sql = "";
		if (concretize) {
			sql = "select " + expression;
			sqlConcretizer.concretizeAll(sql, context);
		}
		DBSConnection con = getConnection(context);
		logSQL(con, sql);
		DataSet rs = con.query(sql, this);
		logStatusSQLResult(con, rs);
		rs.next();
		boolean r = (Boolean) rs.currentRecord().getValue(1);
		if (rs.next())
			throw new RuntimeException("Consulta retorna mais de um registro");
		rs.close();
		return r;
	}

	protected void showLabel(Context context) {
		Record r = null;
		if (context != null) {
			if (context.dataSet != null)
				r = context.dataSet.currentRecord();
			if (r == null)
				r = context.record;
		}
		for (String s : labels) {
			if (s.contains("@"))
				s = defaultConcretizer.concretizeAll(s, context);
			println(s);
		}
	}

	private Integer foundRecordsThreshold;

	public Integer getFoundRecordsThreshold() {
		return foundRecordsThreshold;
	}

	public void setFoundRecordsThreshold(Integer threshold) {
		if ((foundRecordsThreshold != null) && (threshold != null)) {
			int f = foundRecordsThreshold;
			if (f != threshold)
				throw new RuntimeException(
						"Divergência no limiar de registros encontrados entre #iffound e #ifnotfound");
		} else
			foundRecordsThreshold = threshold;
	}

	protected long delayTimeBefore = 0;
	protected long delayTimeAfter = 0;

	@Deprecated
	public void assignFoundRecordsThreshold(int threshold) {
		if (foundRecordsThreshold != null) {
			int f = foundRecordsThreshold;
			if (f != threshold)
				throw new RuntimeException(
						"Divergência no limiar de registros encontrados entre #iffound e #ifnotfound");
		} else
			foundRecordsThreshold = threshold;
	}

	protected Future<DataSet> asyncQueryDataSet(String sql, Performer invoker, Context context) {
		Future<DataSet> r = DBS.parallelizer().call(() -> queryDataSet(sql, invoker, context));
		return r;
	}

	protected Future<DataSet> syncQueryDataSet(String sql, Performer invoker, Context context) {
		DataSet d;
		try {
			d = queryDataSet(sql, invoker, context);
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}

		Future<DataSet> r = new Ready<DataSet>(d);
		return r;
	}

	protected DataSet queryDataSet(String sql, Performer invoker, Context context) throws SQLException, IOException {
		Date t = new Date();
		String connId = getConcreteConnectionId(context);
		DataSet r = query(connId, sql, this);
		r.setInvokerData(sql, context, invoker.getAlias());
		println("Consulta realizada em " + Util.elapsedTimeText(t));
		System.out.println();
		return r;
	}

//	public String getExecutionThreadId() {
//		return executionThreadId;
//	}

	public Boolean isAutoCommit() {
		return autoCommit;
	}

	public synchronized Result execute(Context invokerContext) {

		if (!satisfiedConditions(invokerContext))
			return null;

		String command = templateCommand.getCommand(invokerContext);
		Result r = perform(command, invokerContext);
		return r;

	}

	private boolean satisfiedConditions(Context context) {
		for (Expression<Boolean> c : conditions) {
			c.concretize(defaultConcretizer, context);
			if (!c.solve())
				return false;
		}
		return true;
	}

	public void delay(long time) {
		if (time == 0)
			return;
		try {

			println("Aguardando " + Util.elapsedTimeText(time) + "...");
			Thread.sleep(time);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

//	@Deprecated
//	public Result execute_Old(DataSet source, Record invokerRecord) throws SQLException, IOException, ParseException {
//
//		if (executionThreadId == null) {
//
//			return synchronizedExecute(source, invokerRecord);
//
//		} else {
//
//			engine.executePerformerThread(this, source, invokerRecord);
//			return null;
//
//		}
//
//	}
//
//	public void setExecutionThreadId(String performerThreadId) {
//		this.executionThreadId = performerThreadId;
//	}

	public Macro getEngine() {
		return engine;
	}

	public DBSConnection getConnection(Context context) {

		if (isDynamicConnection())
			throw new RuntimeException("Não é possível identificar uma conexão dinâmica sem informar o contexto");
		String id = getConcreteConnectionId(context);
		DBSConnection r = getProgram().getConnection(id);
		Language lang = r.getLanguage();
		sqlConcretizer.setLanguage(lang);
		return r;
	}

	public DBSConnection getConnection() {
		if (!isDynamicConnection())
			return getProgram().getConnection(templateConnectionId, defaultConcretizer);
		return getConnection(null);
	}

	public StringConcretizer getSuitableConcretizer(Context context) {
		DBSConnection connection = getConnection(context);
		if (connection instanceof JDBCConnection)
			return sqlConcretizer;
		else
			return defaultConcretizer;
	}

	protected void incRecordCount(int inc) {
		defaultIncRecordCount(inc);
	}

	protected void defaultIncRecordCount(int inc) {
		recordCount += inc;
		totalRecordCount += inc;
	}

	public int getRecordCount() {
		return recordCount;
	}

	public int getTotalRecordCount() {
		return totalRecordCount;
	}

	public int getExecution() {
		return execution;
	}

	protected abstract boolean showDataRow();

	public abstract void recursivelyGenerateImplicitSlaves();

//	public DBSConnection getPrivateConnection() {
//		return privateConnection;
//	}
//
//	public void setPrivateConnection(DBSConnection privateConnection) {
//		this.privateConnection = privateConnection;
//	}

	public abstract boolean repeat();

	public abstract void setRepeat(boolean repeat);

	public void defaultImportRow(DataSet source, Record bufferedRecord) {
		// Nada. Apenas TargetPerformer possui implementação.
	}

	public abstract void performDefaultStartImportingData(DataSet dataSet, Context context);

	public abstract void performDefaultEndImportingData(DataSet dataSet, Context context);

	@Override
	public String toString() {
		String a = getSimpleName();
		a = Check.coalesce(a, getFullName());

		return a + " [templateCommand~=" + templateCommand + "]";
	}

	public String getTemplateCommand(Context context) {
		return templateCommand.getCommand(context);
	}

	public String[] getFieldsToFilter(Context context) {
		return null;
	}

	public static final String PRIVATE_CONNECTION_ID = "*private-connection*";

	protected static final String DEFAULT_IMPORT = "default import";

	protected void showTableRecord(String prefix, Record record) {
		if (record == null) {
			println(prefix, "(registro nulo)");
			return;
		}

		StringBuilder row = new StringBuilder();
		for (int i = 0; i < record.getFieldCount(); i++) {
			Object value = record.getValue(i);
			if (row.length() > 0)
				row.append(columnSeparator);
			row.append(value);
		}
		String s = row.toString();
		row.setLength(0);
		row.trimToSize();
		showRow(prefix, s);
	}

	private void showRow(String prefix, String row) {
		println(prefix, row);
	}

	protected void showHeader(String prefix, DataSet source) {
		StringBuilder row = new StringBuilder();
		for (String name : source.getFieldNames()) {
			if (row.length() > 0)
				row.append(columnSeparator);
			row.append(name);
		}
		String s = row.toString();
		row.setLength(0);
		row.trimToSize();
		showRow(prefix, s);

	}

//	@Override 
//	public void log(String... strings) {
//		println(strings);
//	}

	public Event getEvent() {
		return event;
	}

	public void triggerAfterLastRowEvent(Context context) {
		if (event == null)
			return;

		if (event.checkAfterLastRow(context.dataSet))
			execute(context);
	}

	public void triggerBeforeFirstRowEvent(Context context) {
		if (event == null)
			return;

		if (event.checkBeforeFirstRow(context.dataSet))
			execute(context);
	}

	public String formatValue(Object value) {
		if (value == null)
			return "";

		if ((value instanceof Double) || (value instanceof Float) || (value instanceof BigDecimal)) {
			return formatDecimalValue((Number) value);
		}

		return value.toString();

	}

	private String formatDecimalValue(Number value) {
		String r = value.toString();

		if (decimalSeparator == null)
			return r;
		if (decimalSeparator.equals("."))
			return r;
		r = r.replace(".", decimalSeparator);
		return r;
	}

	public void notifyInvokerRequestedDataSet(SourcePerformer invoker, Context invokerContext,
			Future<DataSet> loadingDataSet) {
		// nada
	}

	protected int totalOperations = 0;

	protected Boolean showPerformance = true;

	private HashMap<Long, PerformanceRecord> monitoringIntervalRecords = new HashMap<Long, PerformanceRecord>();

//	@Deprecated
//	private static Waiter waiter = new DefaultWaiter(System.out);
//	static Waiter waiter = new NoWaiter();

	static final long INTERVAL_15_SECONDS = 15000;

	static final long INTERVAL_5_MINUTES = 300000;

	static final long INTERVAL_1_HOUR = 3600000;

	// private static final DecimalFormat defaultDecimalFormat = new
	// DecimalFormat("#.###");

	// private Date lastPerformanceReported = new Date();

	// private long elapsedSinceLastPerformanceReported(Date currentTime) {
	// return currentTime.getTime() - lastPerformanceReported.getTime();
	// }

	private void showPerformanceMsg(Long monitoringInterval, long currentTime) {
		PerformanceRecord rec;

		int trc = getTotalRecordCount();

		if (monitoringInterval != null) {
			rec = monitoringIntervalRecords.get(monitoringInterval);
			if (rec == null) {
				rec = new PerformanceRecord(currentTime, totalOperations, trc);
				monitoringIntervalRecords.put(monitoringInterval, rec);
			}

		} else {
			rec = new PerformanceRecord(getProgram().getStartTime().getTime(), 0, 0);
			rec.setPreviousSpentTime(getProgram().getPreviousSpentTime());
		}

		long elapsed = rec.elapsedTime(currentTime);

		if (monitoringInterval != null)
			if (elapsed > monitoringInterval)
				monitoringIntervalRecords.remove(monitoringInterval);

		String a;
		if (monitoringInterval != null)
			a = Util.elapsedTimeText(monitoringInterval);
		else
			a = "Global";

		String t = "";
		if (a.length() <= 2)
			t = "\t";

		if (elapsed > 0)
			println("Performance em ", a, ": \t" + t, rec.operationsRateText(currentTime), "\t",
					rec.recordsRateText(currentTime));
		else
			println("Performance em ", a, ": em apuração");
	}

	private class PerformanceRecord {

		long start;
		long previousSpentTime = 0;
		int startOperations;
		int startRecords;

		public PerformanceRecord(long start, int operations, int records) {
			super();
			this.start = start;
			this.startOperations = operations;
			this.startRecords = records;
		}

		public void setPreviousSpentTime(long time) {
			this.previousSpentTime = time;
		}

		long elapsedTime(long currentTime) {
			return currentTime - start + previousSpentTime;
		}

		@Override
		public String toString() {
			return "PerformanceRecord [start=" + start + ", previousSpentTime=" + previousSpentTime
					+ ", startOperations=" + startOperations + ", startRecords=" + startRecords + "]";
		}

		int performedOperations() {
			return totalOperations - startOperations;
		}

		int performedRecords() {
			return getTotalRecordCount() - startRecords;
		}

		double operationsRate(long currentTime) {
			double r = performedOperations() * 1000 / (elapsedTime(currentTime) + 0.0);
			if (r < 0)
				println("performedOperations() = " + performedOperations() + ", elapsedTime(currentTime) = "
						+ elapsedTime(currentTime));
			return r;
		}

		double recordsRate(long currentTime) {
			int pr = performedRecords();
			long et = elapsedTime(currentTime);
			double r = pr * 1000 / (et + 0.0);
			if (r < 0)
				println("performedRecords() = " + pr + ", elapsedTime(currentTime) = " + et);
			return r;
		}

		String operationsRateText(long currentTime) {
			double r = operationsRate(currentTime);
			return timeRateText(r, "op");
		}

		private String timeRateText(double r, String unit) {
			if (r <= 0.0)
				return "(impossível calcular)";
			if (r >= 10.0)
				return String.format("%.1f " + unit + "/s", r);
			r = r * 60;
			if (r >= 10.0)
				return String.format("%.1f " + unit + "/min", r);
			r = r * 60;
			if (r >= 10.0)
				return String.format("%.1f " + unit + "/h", r);
			r = r * 24;
			return String.format("%.1f " + unit + "/dia", r);
		}

		String recordsRateText(long currentTime) {
			double r = recordsRate(currentTime);
			return timeRateText(r, "reg");
		}

	}

	public void reportPerformance() {
		long currentTime = (new Date()).getTime();
		println();
		println("Exibindo performance para " + getName());
		showPerformanceMsg(INTERVAL_15_SECONDS, currentTime);
		showPerformanceMsg(INTERVAL_5_MINUTES, currentTime);
		showPerformanceMsg(INTERVAL_1_HOUR, currentTime);
		showPerformanceMsg(null, currentTime);
		println();
	}

	@Override
	public String getName() {
		return getFullName();
	}

	@Override
	public String getFullName() {
		return getFullPath(PathField.NAME, ".");
	}

	public enum PathField {
		NAME, ALIAS
	};

	public String getFullPath(PathField field, String separator) {
		String c;
		if (invoker != null)
			c = invoker.getFullPath(field, separator);
		else {

//			if (includeArgs)
//				c = getProgram().calledNameWithArgs(false, " ", false);
//			c = getProgram().calledNameWithArgs(false, " ", false);
//			else

			c = engine.getName();
		}
		if (field == PathField.NAME)
			return c + separator + getSimpleName();
		else if (field == PathField.ALIAS) {
			if (alias == null)
				return c;
			else
				return c + separator + getAlias();
		} else
			throw new RuntimeException("Erro interno");
	}

	public DBS getProgram() {
		return engine.getProgram();
	}

	public void showArgsScene(int offset) {
		DBS program = getEngine().getProgram();
		if (program.mainArgs.length > program.arguments.size() + 1)
			println("[" + Util.concat(program.mainArgs, ", ") + "]");

		showArgByNameScene(offset);
	}

	private void showArgByNameScene(int offset) {
		DBS program = getEngine().getProgram();
		if (program.arguments.size() == 0)
			return;

		StringBuilder r = new StringBuilder();
		r.append("[");
//		Map<String, String> an = program.argByName;
//		if (an == null)
//			return;

		for (Argument a : program.arguments) {
			if (r.length() > 1)
				r.append(", ");

			String value = a.getValue(UndefinedArgAction.NULL);
			if ((value != null) && (value.length() > 40))
				value = value.substring(0, 40) + "(...)";
			r.append(a.getName() + "=" + value);
		}
		r.append("]");
		String s = r.toString();
		r.setLength(0);
		r.trimToSize();
		if (s.isEmpty())
			return;
		println(s);
	}

	@Override
	public String getSimpleName() {
		return simpleName;
	}

	@Deprecated
	public String getSimpleName_old() {
		if (invoker == null)
			return "source";

		int i = -1;
		if (invoker instanceof SourcePerformer)
			i = ((SourcePerformer) invoker).indexOfSlave(this);

		if (i == -1) {
			if (invoker.actionIfFound == this)
				return "iffound";
			if (invoker.actionIfNotFound == this)
				return "ifnotfound";
			for (String k : invoker.actionByError.keySet()) {
				if (invoker.actionByError.get(k) == this)
					return "iferror." + k;
			}
			throw new RuntimeException("Performer não associado");
//			return "unassigned-performer";
		}

		String r = getTypeName();
		if (i > 1)
			r += (i + 1);
		return r;
	}

	protected abstract String getTypeName();

	public boolean clearUnknownVarReferences() {
		return clearUnknownVarReferences;
	}

	public void setClearUnknownVarReferences(boolean clearUnknownVarReferences) {
		this.clearUnknownVarReferences = clearUnknownVarReferences;
	}

	@Override
	public void restoreSavePointProperty(String property, String value) {
		if ("$totalRecordCount".equals(property))
			totalRecordCount = Integer.parseInt(value);

	}

//	public Waiter getWaiter() {
//		return waiter;
//	}

	private boolean savePointDone = false;
	private DataSet previousSubqueryResult;

	@Override
	public void setAsDone() {
		this.savePointDone = true;
	}

	public void setAsDone(boolean done) {
		this.savePointDone = done;
	}

	@Override
	public boolean isDone() {
		return savePointDone;
	}

	protected void registerSavePointAsDone() {
//		if (savePointColumns == null)
//			return;

		if (getProgram().savePoint != null) {
			getProgram().savePoint.registerAsDone(getFullName());
			getProgram().savePoint.save();
		}
	}

	@Override
	public Device findDeviceRel(String relName) {
		String[] ss = relName.split("\\.");
		int prefixLength = ss[0].length() + 1;
		Device sl = null;
		if (ss[0].equals("iffound"))
			sl = actionIfFound;
		else if (ss[0].equals("ifnotfound"))
			sl = actionIfNotFound;
		else if (ss[0].equals("iferror"))
			if (ss.length > 1) {
				sl = (Device) actionByError.get(ss[1]);
				prefixLength += ss[1].length() + 1;
			} else {
				throw new RuntimeException("iferror sem especificação do erro");
			}

		if (sl == null)
			return null;

//		if (sl == null)
//			throw new RuntimeException("Componente não identificado: " + 
//					relName.substring(1, prefixLength - 1));

		if (ss.length == 1)
			return sl;

		relName = relName.substring(prefixLength);
		return sl.findDeviceRel(relName);
	}

	String childId(Performer performer) {
		if (performer == actionIfFound)
			return "iffound";
		if (performer == actionIfNotFound)
			return "ifnotfound";
		if (actionByError.containsValue(performer))
			return "iferror...";
		return null;
	}

	public void showTree() {
		String s = Util.repeat("\t", depth());

		String evs = "";
		if (event != null)
			evs = " [on " + event.toString() + "]";

		templateCommand.showTree(s + getFullName() + evs);

		if (actionIfFound != null) {
			System.out.println(s + "iffound: ");
			actionIfFound.showTree();
		}

		if (actionIfNotFound != null) {
			System.out.println(s + "ifnotfound: ");
			actionIfNotFound.showTree();
		}

		if (actionByError.size() > 0) {
			for (String k : actionByError.keySet()) {
				SimplePerformer a = actionByError.get(k);
				System.out.println(s + "iferror<" + k + ">: ");
				a.showTree();
			}
		}

	}

	public DataSet getPreviousSubqueryResult() {
		return previousSubqueryResult;
	}

	public String getSubqueryColumnSeparator() {
		return subqueryColumnSeparator;
	}

	public String getSubqueryRowSeparator() {
		return subqueryRowSeparator;
	}

	void callExternalProgram(String command, Context context) {

		command = defaultConcretizer.concretizeAll(command, context);

		try {
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public StringConcretizer getDefaultConcretizer() {
		return defaultConcretizer;
	}

	public abstract SavePoint getExceptionCurrentSavePoint(Exception e);

	protected void executeActionsBasedOnRecordsFound(Context invokerContext) {
		boolean found = foundRecords();

		if (found) {
			executeActionIfRecordsFound(invokerContext);
		} else {
			executeActionIfNoRecordsFound(invokerContext);
		}
	}

	public void executeSubProgram(String argsText, Context context) {
		argsText = defaultConcretizer.concretizeAll(argsText, context);
		String[] args = argsText.split("\\s+");
		DBS sub = new DBS(args);
		sub.execute();
	}

	public void setTemplateCommand(String command) {
		if (templateCommand instanceof NoCommand)
			templateCommand = new StaticCommand(this, command);
		if (templateCommand instanceof StaticCommand)
			templateCommand = new StaticCommand(this, command);
		if (templateCommand instanceof ConditionalCommand)
			throw new RuntimeException("Impossível reverter um ConditionalCommand para StaticCommand");
	}

	public void addConditionalCommand(String command, Expression<Boolean> condition) {
		if (templateCommand instanceof NoCommand)
			templateCommand = new ConditionalCommand(this);

		if (templateCommand instanceof StaticCommand) {
			templateCommand = new ConditionalCommand(this);
		}

		ConditionalCommand c = (ConditionalCommand) templateCommand;
		c.addAlternative(command, condition);
	}

	public void reportIdentifiedRecords(String type, int recordCount, boolean ommitZeroRecords, String complement) {
		String r = "";
		if (recordCount > 1 || recordCount < 0) {
			if (!"".equals(type))
				type += "s ";
			r = "-> " + recordCount + " registros " + type + "identificados.";
		} else {
			if (!"".equals(type))
				type += " ";
			if (recordCount == 1)
				r = "-> 1 registro " + type + "identificado.";
			else if (!ommitZeroRecords)
				r = "-> Nenhum registro " + type + "identificado.";
		}

		if ("".equals(r))
			return;

		if (complement != null)
			r += " " + complement;
		println(r);
	}

	public void resetRecordsFound() {
		defaultResetRecordsFound();
	}

	protected void defaultResetRecordsFound() {
		setRecordCount(0);
	}

	public Logger getLogger() {
		return engine.getProgram().getLogger();
	}


}
