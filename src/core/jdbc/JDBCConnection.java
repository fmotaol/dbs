package core.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import core.DBS;
import core.Macro;
import core.dataset.DataSet;
import core.performer.Batch;
import core.performer.Context;
import core.performer.DBSConnection;
import core.performer.Performer;
import core.performer.Result;
import core.performer.TargetPerformer;
import core.sql.Language;
import ext.db.DriverSupport;
import ext.db.db2.DB2Dialet;
import ext.db.pgsql.PostgreSQLDialet;
import util.Util;
import util.threads.Parallelizer;

public class JDBCConnection extends DBSConnection {

	private String id;

	@Override
	public String getId() {
		return id;
	}

	private Connection connection;
	
	Parallelizer parallel = new Parallelizer(2);

	private DBProperties properties;

	private Language language;

	DriverSupport driverSupport;

	private boolean reconnect;

	private HashMap<String, String[]> pkFieldsByTable = new HashMap<String, String[]>();

	private HashMap<String, String[]> fieldsByTable = new HashMap<String, String[]>();
	
	private Statement activeStatement;
	
//	private WarningChecker warningChecker; 

	public JDBCConnection(DBProperties properties, String id) {
		this.id = id;
		this.properties = properties;
		createDriverConnection();
	}

	private void createDriverConnection() {
		String driver = properties.getDriver();
		String url = properties.getUrl();
		language = createLanguage(driver, url);

		if (driver != null) {
			try {

				Class.forName(driver);

			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		
		driverSupport = DriverSupport.createSupport(driver, url);
		
		connect();
	}

	static Language createLanguage(String driver, String url) {
		if (driver != null) {
			if (driver.equals("com.ibm.db2.jcc.DB2Driver"))
				return new DB2Dialet();

			return defaultLanguage(driver);
		}

		if (url == null)
			throw new RuntimeException("Driver nem URL informados");

		if (url.startsWith("jdbc:postgresql") || url.startsWith("jdbc:pgsql"))
			return new PostgreSQLDialet();

		if (url.startsWith("jdbc:db2"))
			return new DB2Dialet();

		return defaultLanguage(driver);
	}

	private static Language defaultLanguage(String driver) {
		if (driver == null)
			driver = "";
		else
			driver = ": " + driver;

		System.out.println("AVISO: Driver não identificado" + driver);
		System.out.println("AVISO: Assumido o suporte de linguagem default");
		// TODO implementar o tratamento via String de driver dos bancos
		// básicos
		return Language.defaultLanguage();
	}

	private void connect() {

		try {
			connection = DriverManager.getConnection(properties.getUrl(), properties.getUser(),
					properties.getPassword());
		} catch (SQLException e1) {
			throw new RuntimeException(e1);
		}

		
		try {
			driverSupport.setupWarningListener(connection);
		} catch (Throwable e) {
			System.out.println("AVISO: Não foi possível associar um listener para Warnings. Mensagens assíncronas poderão não ser exibidas.");
			if (e.getMessage() != null)
				System.out.println("Erro: " + e.getMessage());
			System.out.println();
		}
		
		try {
			connection.setClientInfo("ApplicationName", DBS.mainProgram.getApplicationAsInvoked());
		} catch (Throwable e) {
			System.out.println("AVISO: Propriedade ApplicationName não foi associada");
			if (e.getMessage() != null)
				System.out.println("Erro: " + e.getMessage());
			System.out.println();
		}
	}

	public static DBSConnection createConnection(DBProperties prop, String connectionId) {
		DBSConnection r = new JDBCConnection(prop, connectionId);
		return r;
	}

	@Override
	public synchronized DataSet query(String sql, Performer invoker) {
		try {
			JDBCDataSet r = internalQuery(sql);
			r.setInvokerData(sql, null, null);
			return r;
		} finally {
			activeStatement = null;
		}
	}

	private JDBCDataSet internalQuery(String sql) {
		JDBCDataSet r;
		try {
			PreparedStatement s = prepareStatement(sql);
			activeStatement = s;
			
			//ResultSet rs = parallel.callAndWait(() -> s.executeQuery());
			ResultSet rs = executeWithWarnings(() -> s.executeQuery(), s);
			//ResultSet rs = s.executeQuery();
			r = new JDBCDataSet(rs, s);
			//warningChecker.finish();
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			activeStatement = null;
		}
		return r;
	}

	@Override
	public void setAutoCommit(Boolean autoCommit) throws SQLException {
		connection.setAutoCommit(autoCommit);
	}

	private String[] loadFieldsByTableName(String tableName) {
		ArrayList<String> list = new ArrayList<String>();
		String sql = "select column_name, data_type from information_schema.columns\r\n" + "where table_name = '"
				+ tableName + "' order by ordinal_position";

		JDBCDataSet rs = (JDBCDataSet) query(sql, null);
		while (rs.next()) {
			list.add((String) rs.currentRecord().getValue("column_name"));
		}
		rs.close();
		String[] r = new String[list.size()];
		r = list.toArray(r);
		list.clear();
		return r;
	}

	private String[] loadPrimaryKeyFields(String table) throws SQLException {
		ArrayList<String> list = new ArrayList<String>();
		String pksql = "select column_name\r\n from information_schema.table_constraints c\r\n"
				+ "join information_schema.constraint_column_usage u on c.constraint_name = u.constraint_name\r\n"
				+ "where constraint_type = 'PRIMARY KEY' and c.table_name ilike '" + table + "';\r\n";

		JDBCDataSet rs = (JDBCDataSet) query(pksql, null);
		boolean found = false;
		while (rs.next()) {
			list.add((String) rs.currentRecord().getValue("column_name"));
			found = true;
		}
		rs.close();
		if (!found)
			throw new RuntimeException("Não foi localizada chave primária para a tabela " + table);
		String[] r = new String[list.size()];
		r = list.toArray(r);
		list.clear();
		return r;
	}

	@Override
	public synchronized Result executeBatch(Batch batch) throws SQLException {
		Result r = parallel.callAndWait(() -> internalExecuteBatch(batch));
		return r;
	}

	private Statement createStatementForBatch(List<String> buffer) throws SQLException {
		Statement statement = createStatement();
		for (String sql : buffer) {
			statement.addBatch(sql);
		}
		return statement;
	}

	private PreparedStatement prepareStatement(String sql) throws SQLException {
		PreparedStatement r = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		assignWarningListener(r);
		return r;

	}

	private void assignWarningListener(Statement stmt) {
		//if (!DBS.DISABLE_NOTICES)
			//warningChecker = new WarningChecker(connection, stmt);
	}

	protected <T> T executeWithWarnings(Callable<T> callable, Statement statement) {
		WarningChecker warningChecker = new WarningChecker(this, statement);
		try {
			
			return callable.call();
			
		} catch (Exception e) {
			
			throw new RuntimeException(e);
			
		} finally {
			
			warningChecker.finish();
			
		}		
	}
	
	protected Result internalExecuteBatch(Batch batch) throws SQLException {
		Statement st = createStatementForBatch(batch.getBuffer());
		
		int[] is = st.executeBatch();
		int affectedRows = 0;
		for (int i : is)
			if (i >= 0)
				affectedRows += i;
		return new Result(affectedRows);
	}

	@Override
	public synchronized Result execute(String sql, Performer invoker, Context context) {

		try {
			return internalExecute(sql);
		} catch (SQLException e) {
			System.out.println("Erro no comando SQL:");
			System.out.println(sql);
			throw new RuntimeException(e);
		}
	}

	private Result internalExecute(String sql) throws SQLException {
		activeStatement = createStatement();

		Result r;
		try {

			//warningChecker = new WarningChecker(this, activeStatement);
			// parallel.callAndWait(() -> activeStatement.execute(sql));
			executeWithWarnings(() -> activeStatement.execute(sql), activeStatement);

			int affectedRows = activeStatement.getUpdateCount();
			if (affectedRows < 0)
				affectedRows = 0;

			ResultSet rs = activeStatement.getResultSet();

			if (affectedRows == 0)
				affectedRows = getRowCountFromResultSet(rs);

			if (rs == null)
				r = new Result(affectedRows);
			else {
				JDBCDataSet ds = new JDBCDataSet(rs, activeStatement);
				r = new Result(affectedRows, ds);
			}

		} finally {
//			activeStatement.close();
			activeStatement = null;
		}
		return r;
	}

	Statement createStatement() throws SQLException {
		Statement r = connection.createStatement();
		assignWarningListener(r);
		return r;
	}

	// public ServerType serverType() {
	// if (properties.driver == null)
	// return ServerType.PosgreSQL;
	//
	// throw new RuntimeException("Tipo de servidor desconhecido: " +
	// properties.driver);
	// }

	private int getRowCountFromResultSet(ResultSet rs) {
		int r = 0;
		try {
			if (rs == null)
				return 0;
			int ci = rs.findColumn("_affectedrows");
			if (ci <= 0)
				return 0;

			while (rs.next()) {
				int c = rs.getInt("_affectedrows");
				r += c;
			}
		} catch (SQLException e) {
			return r;
		}

		return r;
	}

	@Override
	public Batch createBatch(Performer performer) {
		return new Batch();
//		return new JDBCBatch(performer.getJDBCConnection());
	}

	@Override
	public void reconnect() {
		try {
			if (connection != null)
				connection.close();
		} catch (Exception e) {
		}

		connect();
	}

	public String[] getTableFieldsByTableName(String tableName) {
		String[] fields = fieldsByTable.get(tableName);
		if (fields == null) {
			fields = loadFieldsByTableName(tableName);
			fieldsByTable.put(tableName, fields);
		}
		return fields;
	}

	public String[] getPrimaryKeyFields(String tableName) throws SQLException {
		if ((tableName == null) || (tableName.trim().isEmpty()))
			throw new RuntimeException("Nome da tabela vazio");

		tableName = tableName.toLowerCase();

		String[] pkFields = pkFieldsByTable.get(tableName);
		if (pkFields == null) {
			pkFields = loadPrimaryKeyFields(tableName);
			pkFieldsByTable.put(tableName, pkFields);
		}
		return pkFields;
	}

	public String inferTableName(String sql) {
		sql = sql.trim();
		if (Util.startsWithIgnoreCase(sql, "update"))
			return inferTableNameFromUpdate(sql);
		if (Util.startsWithIgnoreCase(sql, "insert"))
			return inferTableNameFromInsert(sql);

		throw new RuntimeException("Não foi possível inferir o nome da tabela em " + sql);
	}

	private String inferTableNameFromUpdate(String sql) {
		String[] ss = sql.split("^update\\s|\\sset\\s");
		if (ss.length < 2)
			throw new RuntimeException("Não foi possível inferir o nome da tabela");

		String r = ss[1].trim();
		return r;
	}

	private String inferTableNameFromInsert(String sql) {
		String[] ss = sql.split("^insert\\sinto\\s|\\s");
		if (ss.length < 2)
			throw new RuntimeException("Não foi possível inferir o nome da tabela");

		String r = ss[1].trim();
		return r;
	}

	public String[] getTableFields(String tableName) throws SQLException {
		return getTableFieldsByTableName(tableName);
	}

	public boolean isInsertOrUpdateCommand(String sql) {
		String s = sql.toLowerCase().trim();
		if (s.startsWith("update"))
			return true;
		if (s.startsWith("insert"))
			return true;

		return false;
	}

	@Deprecated
	@Override
	public JDBCConnection getJDBCConnection() {
		return this;
	}

	@Override
	public void defaultStartImportingData(TargetPerformer performer) {
		// nada
	}

	@Override
	public void defaultImportRow(TargetPerformer performer, Context context) {
		String update = language.getUpdateTemplate();
		String insert = language.getInsertTemplate();
		performer.executeTemplate(update, context);
		performer.executeTemplate(insert, context);
	}

	@Override
	public void defaultEndImportingData(TargetPerformer performer) {
		// nada
	}

	private long sleepTime = 0;
	private static final int T_30_MIN = 30 * 60 * 1000;

	public boolean tryToReconnect() throws SQLException {
		if (!reconnect)
			return false;

		if (sleepTime > 0)
			Macro.println("Aguardando " + (sleepTime / 1000), " segundos...");

		sleep(sleepTime);

		sleepTime = (long) (sleepTime * 1.5) + 1000;
		if (sleepTime > T_30_MIN)
			sleepTime = T_30_MIN;

//		try {
		Macro.println("Tentando reconectar...");
		reconnect();
		sleepTime = 0;
		return true;

//		} catch (SQLException e2) {
//			throw e2;
//		}
	}

	@Override
	public String toString() {
		return properties.url;
	}

//	@Override
//	public boolean hasNotice() {
//		if (activeStatement == null)
//			return false;
//
//		try {
//			if (activeStatement.isClosed())
//				return false;
//
//			SQLWarning w = activeStatement.getWarnings();
//			if (w == null)
//				return false;
//
//			return true;
//		} catch (SQLException e) {
//			return false;
//		}
//	}

//	@Override
//	public String consumeNotice() {
//		if (activeStatement == null)
//			throw new RuntimeException("Não existe statement ativo");
//
//		try {
//			if (activeStatement.isClosed())
//				throw new RuntimeException("Não existe statement aberto");
//
//			SQLWarning w = activeStatement.getWarnings();
//			if (w == null)
//				throw new RuntimeException("Não existem notices");
//
//			String s = w.getMessage();
//
//			activeStatement.clearWarnings();
//
//			return s;
//		} catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
//	}

	@Override
	public void close() {
		try {

			if (activeStatement != null)
				activeStatement.cancel();

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		try {

			connection.close();
			connection = null;

		} catch (SQLException e) {
		}
		
//		try {
//
//			closeExecutorService();
//
//		} catch (Throwable e) {
//			e.printStackTrace();
//		}
		
	}

	public Language getLanguage() {
		return language;
	}

//	private static ExecutorService threadExecutorService;

//	static ExecutorService getWarningExecutorService() {
//		if (threadExecutorService == null)
//			threadExecutorService = Executors.newFixedThreadPool(2);
//
//		return threadExecutorService;
//	}

	
    public void closeParallelizer() {
        if (parallel != null) {
        	parallel.shutdown();
//            try {
//                if (!parallel.awaitTermination(5, TimeUnit.SECONDS)) {
//                    threadExecutorService.shutdownNow();
//                }
//            } catch (InterruptedException e) {
//                threadExecutorService.shutdownNow();
//                Thread.currentThread().interrupt();
//            }
        }
    }

}
