package core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;

import core.Argument.Origin;
import core.SavePoint.DataScope;
import core.file.FileConnection;
import core.jdbc.DBProperties;
import core.jdbc.JDBCConnection;
import core.parsing.CommandParser;
import core.parsing.replace.StringConcretizer;
import core.performer.DBSConnection;
import core.performer.DefaultConnection;
import core.performer.Performer;
import core.util.FileLogger;
import core.util.Logger;
import ext.http.HTTPConnection;
import ext.path.json.JsonPathConnection;
import ext.path.xml.XPathConnection;
import util.Args;
import util.FileUtil;
import util.Maths;
import util.Strings;
import util.Util;
import util.console.ANSI;
import util.logical.Assert;
import util.threads.ParallelProcessor;

public class DBS extends Engine {

	public static final boolean DISABLE_NOTICES = false;

	public static DBS mainProgram;

	private String dbsFile = null;

	public String[] mainArgs;

	public ArrayList<Argument> arguments = new ArrayList<>();

	private Macro[] engines = null;

	public boolean restart = false;

	private boolean error = false;

	public Date startTime = new Date();

	private long previousSpentTime = 0;

	public boolean recursiveReference = false;

	public boolean ignoreUnknownFields = false;

	public boolean usePreparedStatements = false;
	
	public Logger logger;

	protected StringConcretizer connectionConcretizer = new StringConcretizer(this);

	public DBS(String[] args) {
		mainArgs = args;
		obtainDBSFile();
	}

	public static void main(String[] args) throws SQLException, IOException, ParseException {
		configureColors(args);

		showIntro();

		mainProgram = new DBS(args);

		try {
			try {
				mainProgram.execute();
			} finally {
				freeParallelProcessor();
			}
		} catch (Throwable e) {
//			throw new RuntimeException(e);
			do {
				e.printStackTrace();
				e = e.getCause();
			} while (e != null);
			System.exit(1);
		}
	}

	private static void configureColors(String[] args) {
		boolean enableColors = false;
		Args as = new Args(args);
		if (as.containsArg("ENABLE_COLORS")) {
			Boolean en = as.get("ENABLE_COLORS", true);
			if (en == null)
				en = true;
			enableColors = en;
		}
		Console.ENABLE_COLORS = enableColors;
	}

	public void execute() {

		if (mainArgs.length > 0) {
			println("Chamada: DBS " + Strings.concat(mainArgs, " "));
			println("Programa: " + dbsFile);
		}

		println();

		String content = loadContentFromFile(dbsFile);

		do {

			clearSessionAttributes();

			parseCommands(content);

			generateAllImplicitTargets();

//			showEnginesTree();

			loadArgs();

			loadSavePointProperties();

			try {

				Date t = new Date();

				executeEngines();

				println("Tempo total: ", Util.elapsedTimeText(t));

			} catch (Exception e) {
				Console.println("Erro: " + e.getMessage());
				error = true;

				if (restart)
					println("Reiniciando...");
				else
					Util.throwAsRuntimeException(e);
//					e.printStackTrace();

			}

		} while (restart);

		if (!error)
			if (savePoint != null)
				savePoint.finish();
	}

	private void loadArgs() {
		loadArgValues();
		if (savePoint != null) {
			if (savePoint.fileExists())
				savePoint.load(DataScope.ARGS);
		}
	}

	private void loadSavePointProperties() {
		SavePoint sp = SavePoint.newSavePoint(this, false); // primeiro busca filename.sav
		if (sp != null)
			if (sp.fileExists())
				sp.load(DataScope.PROPERTIES);

		if (savePoint != null) // agora busca filename#args....sav
			if (savePoint.fileExists())
				savePoint.load(DataScope.PROPERTIES);
	}

	class Console extends ANSI {};
	
	private static void showIntro() {

		Console.setColor(ANSI.RED);
		Console.println();
		Console.println();
		Console.println();
		Console.println();
		Console.println("             #                ###                #            ###     ### ");
		Console.println("            # #              #####              # #         ####### ####### ");
		Console.println("           #   #              ###              #   #       ################# ");
		Console.println("         #       #       ###   #   ###        #     #      ################# ");
		Console.println("        #         #     ###############      #       #      ############### ");
		Console.println("        #   ###   #      ###   #   ###        #     #         ########### ");
		Console.println("         ### # ###             #               #   #            ####### ");
		Console.println("             #                ###               # #               ###  ");
		Console.println("            ###              #####               #                 # ");
		Console.println();
		Console.resetColor();
		Console.println("######################################################################################");
		Console.println("##########################        #####        #####       ###########################");
		Console.println("##########################   ###   ####   ###   ###   ###   ##########################");
		Console.println("##########################   ####   ###   ###   ###   ################################");
		Console.println("##########################   ####   ###       #######     ############################");
		Console.println("##########################   ####   ###   ###   #########   ##########################");
		Console.println("##########################   ###   ####   ###   ###   ###   ##########################");
		Console.println("##########################        #####        #####       ###########################");
		Console.println("######################################################################################");
		Console.println();
		Console.println("             #            ###     ###            #                ### ");
		Console.println("            ###         #     # #     #         ###              #   # ");
		Console.println("           #####       #       #       #       #####              ### ");
		Console.println("          #######      #               #     #########       ###   #   ### ");
		Console.println("         #########      #             #     ###########     #   #######   # ");
		Console.println("          #######         #         #       ###########      ###   #   ### ");
		Console.println("           #####            #     #          ### # ###             # ");
		Console.println("            ###               # #                #                # # ");
		Console.println("             #                 #                ###              ##### ");
		Console.println();
		Console.println();
		Console.println("                                    DataBase Synchronizer");
		Console.println();
		Console.println("     Criado por Fabricio Mota           ");
		Console.println();

	}

	private static int threadPoolSize = 4;

	private static ParallelProcessor parallelProcessor;

	private static void freeParallelProcessor() {
		if (parallelProcessor != null)
			parallelProcessor.shutdown();
	}

	public static ParallelProcessor parallelProcessor() {
		if (parallelProcessor == null)
			parallelProcessor = new ParallelProcessor(threadPoolSize);
		return parallelProcessor;
	}

	public static void setThreadPoolSize(int threadPoolSize) {
		DBS.threadPoolSize = threadPoolSize;
		if (parallelProcessor != null) {
			ParallelProcessor old = parallelProcessor;
			parallelProcessor = new ParallelProcessor(threadPoolSize);
			old.shutdown();
		}
	}

	public static int getThreadPoolSize() {
		return threadPoolSize;
	}

	private void loadArgValues() {
		parseCommandLineArgByName(mainArgs);
		loadFromArgsFile();
	}

	private void obtainDBSFile() {
		if ((mainArgs.length > 0) && (mainArgs[0].length() > 0))
			setOpenedFile(mainArgs[0]);
		else
			queryForDBSFile();
	}

	private void parseCommandLineArgByName(String[] args) {
		for (int i = 1; i < args.length; i++) {
			String a = args[i];
			if (a == null)
				continue;

			if (a.startsWith("--")) { // abordagem ... arg
				if (args.length <= i)
					throw new RuntimeException("Faltando argumento para " + a);
				String v = args[i + 1];
				if (v.startsWith("--"))
					v = "true"; // se n�o h� o valor correspondente - interpreta-se como uma assertiva
				else
					i++; // pula um, pq j� consumiu o valor do arg

				String n = a.substring(2);
				Argument arg = getArgByName(n);
				if (arg.origin == null)
					arg.setValue(v, Origin.PROGRAM_INPUT);
				else
					Console.println("AVISO: argumento de programa " + arg.getName() + " ignorado. Já carregado de "
							+ arg.origin);
				continue;
			}

			if (a.contains("=")) {
				String[] as = a.split("=");
				if (as.length > 1) {
					String v = a.substring(as[0].length() + 1);
					Argument arg = getArgByName(as[0]);
					if (arg.origin == null)
						arg.setValue(v, Origin.PROGRAM_INPUT);
					else
						Console.println("AVISO: argumento " + arg.getName() + " já carregado de " + arg.origin);
				}
				continue;
			}

			createArg(i, a);

		}
	}


	public Argument getArgByName(String name) {
		for (Argument a : arguments) {
			if (name.equals(a.getName()))
				return a;
		}
		return null;
	}

	private void loadFromArgsFile() {
		File argsFile = new File(getDBSFileName() + ".args");

		if (argsFile.exists()) {
			ArgsFileLoader afl = new ArgsFileLoader(this, argsFile);
			try {
				afl.loadContent();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public String getDBSFileName() {
		File f = new File(dbsFile);
		String fileName = f.getName();
		int i = fileName.lastIndexOf('.');
		if (i > 0)
			return fileName.substring(0, i);
		else
			return fileName;
	}

	public String getDBSFileExt() {
		return FileUtil.fileExt(dbsFile);
	}

	public String getDBSFilePath() {
		File f = new File(dbsFile);
		String r = f.getAbsolutePath();
		return r;
	}

	public String getDBSFileNameWithExt() {
		return getDBSFileName() + getDBSFileExt();
	}

	public void setOpenedFile(String file) {
		String ext = FileUtil.fileExt(file).toLowerCase();
		if (!".sql".equals(ext) && !".dbs".equals(ext))
			if (".sav".equalsIgnoreCase(ext)) {
				loadDBSFileFromSaveFile(file);
				return;
			} else
				throw new RuntimeException("Extens�o de arquivo inv�lida: " + file);

		this.dbsFile = file;
	}

	private void loadDBSFileFromSaveFile(String fileName) {
		savePoint = new SavePoint(this, new File(fileName));
		Console.println("Carregando dados iniciais do arquivo " + fileName);
		savePoint.load(DataScope.DBSFILE);
	}

	public Macro addNewEngine(String macroName) {
		if (findEngine(macroName) != null)
			throw new RuntimeException("J� existe macro chamada " + macroName);
		Macro[] n = new Macro[engines.length + 1];
		for (int i = 0; i < engines.length; i++) {
			n[i] = engines[i];
		}
		Macro e = new Macro(this, macroName);
		n[engines.length] = e;
		engines = n;
		return e;
	}

	public Macro findEngine(String macroName) {
		for (int i = 0; i < engines.length; i++) {
			Macro e = engines[i];
			if (e.getName().equals(macroName))
				return e;
		}
		return null;
	}

	public Macro[] getEngines() {
		return engines;
	}

	public Device findDeviceInAll(Predicate<Device> criteria) {
		Device r = null;
		for (int i = 0; i < engines.length; i++) {
			Device n = engines[i].findDevice(criteria);
			if (r != null) {
				if (n != null)
					if (r != n)
						throw new RuntimeException("Duplicidade de devices para o mesmo crit�rio");
			} else
				r = n;
		}
		return r;
	}

	void showEnginesTree() throws SQLException, IOException, ParseException {

		for (Macro e : engines) {
			e.showTree();
		}

		try {
			Thread.sleep(20000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	public String getApplicationAsInvoked() {
		String r = "DBS";
		if (mainArgs == null)
			return r;
		if (mainArgs.length == 0)
			return r;

		int i = 0;

		for (String a : mainArgs) {
			if (i == 0) {
				File file = new File(a);
				String s = file.getName();
				if (s.endsWith(".sql"))
					s = s.substring(0, s.length() - 4);
				r += " " + s;
			} else
				r += " " + a;
			i++;
		}

		return r;
	}

	private void queryForDBSFile() {
		String dbsfile = inputQuery("Informe o caminho do arquivo DBS/SQL: ");

		if ("".trim().equals(dbsfile) || (dbsfile == null))
			throw new RuntimeException("Arquivo DBS/SQL não informado");

		mainArgs = new String[] { dbsfile };

		setOpenedFile(dbsfile);

	}

	private void clearSessionAttributes() {
		engines = new Macro[0];
		restart = false;
	}

	private void generateAllImplicitTargets() {
		for (Macro e : engines)
			e.generateImplicitSlaves();
	}

	private HashMap<String, DBSConnection> connectionById = new HashMap<String, DBSConnection>();

	private static DefaultConnection defaultConnection = new DefaultConnection();

	static DBSConnection getDefaultConnection() {
		return defaultConnection;
	}

	public DBSConnection getConnection(String connectionId, StringConcretizer concretizer) {
		connectionId = concretizer.concretizeAll(connectionId);
		return getConnection(connectionId);
	}

	public DBSConnection getConnection(String connectionId) {
		if (connectionId == null)
			return getDefaultConnection();

		if (connectionById.get(connectionId) == null) {

			DBSConnection conn = createConnection(connectionId);

			connectionById.put(connectionId, conn);
		}

		DBSConnection r = connectionById.get(connectionId);
		if (r == null)
			throw new RuntimeException("Conexão não identificada: " + connectionId);
		return r;
	}

	private DBSConnection createConnection(String connectionId) {
		if (connectionId.equalsIgnoreCase("dummy"))
			return new DummyConnection();
		if (connectionId.equalsIgnoreCase("files"))
			return new FileConnection();
		if (connectionId.equalsIgnoreCase("http"))
			return new HTTPConnection();
		if (connectionId.equalsIgnoreCase("jsonpath"))
			return new JsonPathConnection();
		if (connectionId.equalsIgnoreCase("xpath"))
			return new XPathConnection();
		
		return createJDBCConnection(connectionId);
	}

	private DBSConnection createJDBCConnection(String connectionId) {
		DBProperties prop = extractDBPropertiesFromConnectionId(connectionId);
		if ((prop.password == null) || prop.password.isEmpty())
			prop.password = queryPassword(prop);

		println("--->>> Conectando a ", connectionId, "...");
		println();

		DBSConnection conn = createJDBCConnection(prop, connectionId);
		// if (prop.reconnectOnError != null)
		// conn = new ReconnectableConnection(conn, prop);

		return conn;
	}

	private DBProperties extractDBPropertiesFromConnectionId(String connectionId) {
//		if (Util.isInteger(connectionId)) { //nota��o antiga e n�o suportada do DBS (quantidade de registros em #iffound = xxx) 
//			throw new RuntimeException(connectionId + " não � uma nota��o v�lida para conex�o");
//		}

		String s = extractFileNameFromConnectionId(connectionId);
		DBProperties prop;
		try {
			prop = DBProperties.newFromFile(s);
			return prop;

		} catch (Exception e) {
			Console.println("Arquivo de propriedades não localizado: " + s);
		}

		try {
			prop = DBProperties.newFromString(s, ";");
			return prop;

		} catch (Exception e) {
			Console.println("ERRO: String de propriedades de conexão não reconhecida: " + s);
			throw new RuntimeException(e);
		}
	}

	private String extractFileNameFromConnectionId(String connectionId) {
		String[] ss = connectionId.split(":");
		return ss[0];
	}

	private static DBSConnection createJDBCConnection(DBProperties prop, String connectionId) {
		return JDBCConnection.createConnection(prop, connectionId);
	}

	public DBSConnection getConnection(String threadId, String connectionId, Boolean autoCommit) throws SQLException {
		DBSConnection c = getConnection(connectionId);
		return c;
	}

	@Deprecated
	public void printPhantom() {
		for (int i = 0; i < engines.length; i++) {
			System.out.print(" ");
		}
		System.out.print(" ");
	}

	private CommandParser parser;

	private void parseCommands(String content) {

//		File f = new File(_commandsFile);
		String hintName = getDBSFileName();
		if (hintName.endsWith(".sql") || hintName.endsWith(".dbs"))
			hintName = hintName.substring(0, hintName.length() - 4);
		parser = new CommandParser(this, content, hintName);
		try {
			parser.parse();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String getCurrentDir() {
		return System.getProperty("user.dir");
	}

	private static String loadContentFromFile(String fileName) {
		File f = new File(fileName);
		String dir = f.getParent();
		String r = null;
		if (dir != null)
			System.setProperty("user.dir", dir);
		FileReader fileReader = newFileReader(f);
		BufferedReader reader = new BufferedReader(fileReader);

		String line = null;

		try {
			do {
				line = readLine(reader);
				if (line != null) {
					if (r != null)
						r = r + "\n" + line;
					else
						r = line;
				}
			} while (line != null);
			return r;
		} finally {
			close(reader);
		}
	}

	private static void close(BufferedReader reader) {
		try {
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}

	private static String readLine(BufferedReader reader) {
		try {
			return reader.readLine();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}

	private static FileReader newFileReader(File f) {
		try {
			return new FileReader(f);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	protected void closeAllConnections() {
		for (String id : connectionById.keySet()) {
			DBSConnection c = connectionById.get(id);
			c.close();
		}
	}

	public Set<String> getConnectionIds() {
		return connectionById.keySet();
	}

	boolean savePointDone = false;

	private void executeEngines() throws SQLException, IOException, ParseException {
//		if (!DISABLE_NOTICES)
//			startNoticeThread();

		for (Macro e : engines) {
			if (savePointDone) {
				continue;
			}
			e.execute();
		}
	}

//	private static void startNoticeThread() {
//		final NoticeListener_Old t = new NoticeListener_Old(null); // TODO associar �s
//															// inst�ncias
//		t.start();
//	}

	public Device findDevice(String fullName) {
		if ((fullName == null) || ("".equals(fullName)))
			return null;

		String[] ss = fullName.split("\\.");

		Macro engine = findEngine(ss[0]);
		if (engine == null)
			throw new RuntimeException("Engine n�o encontrado: " + ss[0]);

		if (ss.length == 1)
			return engine;

		String relName = fullName.substring(ss[0].length() + 1);
		return engine.findDeviceRel(relName);
	}

	@Override
	public String toString() {
		return "DBS [file=" + getDBSFileNameWithExt() + "]";
	}

	public Date getStartTime() {
		return startTime;
	}

	static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	static SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public long getPreviousSpentTime() {
		return previousSpentTime;
	}

	public long getCurrentSpentTime() {
		long r = (new Date()).getTime() - startTime.getTime();
		return r;
	}

	public static String formatTime() {
		return formatTime(new Date());
	}

	public static String formatTime(Date d) {
		String r = simpleTimeFormat.format(d);
		return r;
	}

	public SavePoint savePoint = null;

	public void restoreStaticSavePointProperty(String property, String value) {
		if ("$previousSpentTime".equals(property))
			previousSpentTime = Long.parseLong(value);

	}

	public Object readStaticSavePointProperty(String property, String value) {
		if ("$previousSpentTime".equals(property))
			return previousSpentTime;

		throw new RuntimeException("Propriedade desconnecida: " + property);
	}

	public Device findDeviceByAlias(String alias) {
		Assert.notNull(alias);
		Predicate<Device> p = new Predicate<Device>() {

			@Override
			public boolean test(Device t) {
				if (t == null)
					return false;
				String a = t.getAlias();
				if (a == null)
					return false;
				return alias.equals(a);
			}
		};
		return findDeviceInAll(p);
	}

	public ArrayList<Performer> allNamedPerformers = new ArrayList<>();

	public Performer findPerformerBySimpleName(String name) {
		for (Performer p : allNamedPerformers) {
			String s = p.getSimpleName();
			if (s != null && s.equals(name))
				return p;
		}
		return null;
	}

	private static Scanner keyboardScanner;

	public static String inputQuery(String msg) {
		if (keyboardScanner == null)
			keyboardScanner = new Scanner(System.in);

		System.out.print(msg);
		return keyboardScanner.nextLine();
	}

	public String queryPassword(DBProperties prop) {
		println();
		print("Digite a senha em ", prop.id, " para o usuario ", prop.user, ": ");
		if (System.console() != null) {
			char[] r = System.console().readPassword("");
			return new String(r);
		} else {
			return inputQuery("");
		}
	}

	public Argument getArg(int index) {
		for (Argument a : arguments) {
			String name = a.getName();
			Integer i = Maths.asInt(name);
			if (i != null)
				if (i == index)
					return a;
		}
		return null;

//		if (i > arguments.size())
//			throw new RuntimeException("Argumento " + i + " n�o existe");
//		return arguments.get(i);
	}

	public String getDBSFile() {
		return dbsFile;
	}

	public void assignSavePoint(boolean withArgs) {
		if (savePoint == null)
			savePoint = SavePoint.newSavePoint(this, withArgs);
	}

	public Argument createArg(String name, String value) {
		Argument a;
		a = new Argument(this);
		a.setName(name);
		a.setValue(value, Origin.PROGRAM_INPUT);
		arguments.add(a);
		return a;
	}

	private Argument createArg(int index, String value) {
		Argument a;
		a = new Argument(this);
		a.setName(index + "");
		a.setValue(value, Origin.PROGRAM_INPUT);
		arguments.add(a);
		return a;
	}

	public Logger getLogger() {
		if (logger == null) {
			logger = new FileLogger(this, new Date());
		}
		return logger;
	}

}
