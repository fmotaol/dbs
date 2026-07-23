package core;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import core.Argument.Origin;
import core.dataset.DataSet;
import core.dataset.Record;
import core.performer.Performer;
import core.performer.Result;
import core.performer.TargetPerformer;
import util.Util;

public class Macro extends Engine implements Device, SavePointRestoreable {

	private String name = null;

	private Performer performer;

	private DBS program;

	private List<TargetPerformer> initTargets = new ArrayList<TargetPerformer>();

	private List<TargetPerformer> finalTargets = new ArrayList<TargetPerformer>();

	private List<TargetPerformer> errorTargets = new ArrayList<TargetPerformer>();

	private void init() {
		for (TargetPerformer t : initTargets) {
			try {
				Result r = t.execute(null);
				assignResultDataAsVars(r);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void doFinal() {
		for (TargetPerformer t : finalTargets) {
			try {
				Result r = t.execute(null);
				assignResultDataAsVars(r);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

	private void doOnError() {
		for (TargetPerformer t : errorTargets) {
			try {
				t.execute(null);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

	public Macro(DBS program, String name) {
		super();
		this.program = program;
		this.name = name;
	}

	static boolean containsArg(String[] args, String s) {
		for (String a : args)
			if (a.equals(s))
				return true;
		return false;
	}

	public void execute() throws SQLException, IOException, ParseException {

		DBS.println("Iniciando operação ", name);
		DBS.println();

		Date t = new Date();

		init();

		try {

			try {
				if (performer != null)
					performer.execute(null);

			} catch (Throwable e) {
				mainError = e;
				doOnError();
				throw e;
			}

			doFinal();

		} finally {

			program.closeAllConnections();

		}

		if (program.savePoint != null)
			program.savePoint.registerAsDone(getName()); // marca que concluiu este engine

		println("Macro ", name, " concluida em ", Util.elapsedTimeText(t));
	}

	public Performer getPerformer() {
		return performer;
	}

	public void setPerformer(Performer performer) {
		this.performer = performer;
	}

	@Override
	public String getName() {
		return name;
	}

	public void assignConfigVar(String var, String value) {
		if (var.equalsIgnoreCase("RECURSIVE_REFERENCE")) {
			boolean v = Boolean.parseBoolean(value);
			program.recursiveReference = v;
			return;
		}
		if (var.equalsIgnoreCase("DISCARD_UNKNOWN_FIELDS")) {
			boolean v = Boolean.parseBoolean(value);
			program.ignoreUnknownFields = v;
			return;
		}
		if (var.equalsIgnoreCase("SHOW_SQL")) {
			boolean v = Boolean.parseBoolean(value);
			Performer.DEFAULT_SHOW_SQL = v;
			return;
		}
		if (var.equalsIgnoreCase("SHOW_SCENE")) {
			boolean v = Boolean.parseBoolean(value);
			Performer.DEFAULT_SHOW_SCENE = v;
			return;
		}
		if (var.equalsIgnoreCase("SHOW_STATUS")) {
			boolean v = Boolean.parseBoolean(value);
			Performer.DEFAULT_SHOW_STATUS = v;
			return;
		}
		if (var.equalsIgnoreCase("LOG_SQL")) {
			boolean v = Boolean.parseBoolean(value);
			Performer.DEFAULT_LOG_SQL = v;
			return;
		}
		if (var.equalsIgnoreCase("DISABLE_COMMANDS")) {
			boolean v = Boolean.parseBoolean(value);
			Performer.DISABLE_COMMANDS = v;
			return;
		}
		if (var.equalsIgnoreCase("THREAD_POOL_SIZE")) {
			int s = Integer.parseInt(value);
			DBS.setThreadPoolSize(s);
			return;
		}
		if (assignArgProperty(var, value))
			return;

		throw new RuntimeException("Vari�vel n�o reconhecida: " + var);
	}

	private boolean assignArgProperty(String var, String value) {
		String[] ss = var.split("\\s+", 2);
		if (ss.length < 2)
			return false;
		String cmd = ss[0];
		String arg = ss[1];

		if (cmd.equalsIgnoreCase("ARG_LABEL")) {
			Argument a = program.getArgByName(arg);
			if (a.label != null)
				throw new RuntimeException("Label j� foi definido para o argumento " + arg);
			a.label = value;
			return true;
		}

		if (cmd.equalsIgnoreCase("ARG_DEFAULT_VALUE")) {
			Argument a = program.getArgByName(arg);
			if (a.defaultValue != null)
				throw new RuntimeException("Valor default j� foi definido para o argumento " + arg);
			a.defaultValue = value;
			return true;
		}

		if (cmd.equalsIgnoreCase("ARG_USE_DEFAULT_VALUE")) {
			Argument a = program.getArgByName(arg);
			if (a.useDefault)
				throw new RuntimeException("Condi��o de uso do valor default j� foi definido para o argumento " + arg);
			boolean v = Boolean.parseBoolean(value);
			a.useDefault = v;
			return true;
		}
		
		if (cmd.equalsIgnoreCase("ARG_VALUE_LIST")) {
			Argument a = program.getArgByName(arg);
			if (a.valueList != null)
				throw new RuntimeException("Lista de valores j� foi definido para o argumento " + arg);
			String[] list = Util.splitByComma(value, true);
			a.valueList = list;
			return true;
		}

		if (cmd.equalsIgnoreCase("FORCE_ARG")) {
//			program.addArgByName(arg, value);
			Argument a = program.getArgByName(arg);
			a.setValue(value, Origin.FORCED);
			System.out.printf("AVISO: Argumento \"%s\" for�ado para valor \"%s\"", arg, value);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			System.out.println();
			return true;
		}

		return false;
	}

	public static boolean isPaused = false;

	// private static void dispatchOutputMessagesBuffer() {
	// while (!msgBuffer.isEmpty()) {
	// String msg = msgBuffer.get(0);
	// msgBuffer.remove(0);
	// System.out.println(msg);
	// }
	// lastOutputMessageTime = new Date();
	// }

	// private static long elapsedTime() {
	// Date now = new Date();
	// long r = now.getTime() - lastOutputMessageTime.getTime();
	// return r;
	// }

	// private static void initKeyboardListener() {
	// Signal.handle(new Signal("INT"), new SignalHandler() {
	// // Signal handler method
	// public void handle(Signal signal) {
	// System.out.println("Got signal" + signal);
	// }
	// });
	// }

	// @Deprecated
	// public static void initKeyboardListener() {
	// KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
	// new KeyEventDispatcher() {
	//
	// @Override
	// public boolean dispatchKeyEvent(KeyEvent ke) {
	// synchronized (DBS.class) {
	// switch (ke.getID()) {
	//
	// case KeyEvent.KEY_RELEASED:
	// if (ke.getKeyCode() == KeyEvent.VK_P) {
	//
	// isPaused = !isPaused;
	//
	// } else if (ke.getKeyCode() == KeyEvent.VK_S) {
	//
	// if (systemOutRefreshTime == 0)
	// systemOutRefreshTime = OUTPUT_REFRESH_TIME;
	// else
	// systemOutRefreshTime = 0;
	//
	// }
	//
	// break;
	// }
	// return false;
	// }
	// }
	// });
	// }

	// @Deprecated
	// public static void checkPaused() {
	//
	// System.out.flush();
	//
	// while (isPaused)
	//
	// throw new RuntimeException("ainda n�o implementado");
	//
	// try {
	//
	// Thread.sleep(500);
	//
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// }

	private HashMap<String, String> variables = new HashMap<String, String>();

	public void writeVar(String name, String value) {
		variables.put(name, value);
	}

	public String readVar(String name) {
		String r = variables.get(name);
		if (r == null)
			return "";
		return r;
	}

	public Set<String> getVarNames() {
		return variables.keySet();
	}

	public void clearVar(String name) {
		variables.clear();
	}

	private static String[] systemVars = { "macro", "dbsfile", "dbsfilepath", "dbsfileext", "args", "now",
			"globalaffectedrows", "errorclass", "errormsg" };

	private static Throwable mainError = null;

	public String[] getSystemVars() {
		return systemVars;
	}

	public Object readSystemVar(String name) {
		if ("macro".equalsIgnoreCase(name))
			return getName();

		if ("args".equalsIgnoreCase(name)) {
			String[] args = new String[program.mainArgs.length - 1];
			Util.copyRight(program.mainArgs, args, 1);
			return Util.concat(args, " ");
		}

		if ("dbsfile".equalsIgnoreCase(name))
			return program.getDBSFileName();

		if ("dbsfilepath".equalsIgnoreCase(name)) {
			String r = program.getDBSFilePath();
			return r;
		}

		if ("dbsfileext".equalsIgnoreCase(name))
			return program.getDBSFileExt();

		if ("globalaffectedrows".equalsIgnoreCase(name))
			return TargetPerformer.globalAffectedRows;

		if ("now".equalsIgnoreCase(name))
			return (new Date()).toString();

		if (mainError != null) {
			if ("errorclass".equalsIgnoreCase(name))
				return mainError.getClass().getName();
			if ("errormsg".equalsIgnoreCase(name))
				return mainError.getMessage();

		}

		throw new RuntimeException("Vari�vel de sistema desconhecida: " + name);
	}

	void showNotice(String connectionId, SQLWarning w) {
		println("Notice[" + connectionId + "]: " + w.getMessage());
	}

	void generateImplicitSlaves() {
		if (performer != null)
			performer.recursivelyGenerateImplicitSlaves();
	}

	@Override
	public void setAsDone() {
		program.savePointDone = true;
	}

	@Override
	public boolean isDone() {
		return program.savePointDone;
	}

	public Device findDevice(Predicate<Device> criteria) {
		Device r = null;
		if (criteria.test(this))
			r = this;

		if (performer == null)
			return r;

		Device n = performer.findDevice(criteria);
		if (n != null) {
			if (r != null)
				if (r != this)
					throw new RuntimeException("Duplicidade de devices com o mesmo crit�rio");
		}
		return n;
	}

	@Override
	public void restoreSavePointProperty(String property, String value) {
		throw new RuntimeException("Chamada inv�lida");
	}

	@Override
	public Device findDeviceRel(String relName) {
		String[] ss = relName.split("\\.");
		Performer p = performer;
		if (performer.getSimpleName().equals(ss[0])) {
			p = performer;
		}

		for (Performer i : initTargets) {
			if (!i.getSimpleName().equals(ss[0]))
				continue;
			p = i;
		}

		for (Performer i : finalTargets) {
			if (!i.getSimpleName().equals(ss[0]))
				continue;
			p = i;
		}

		for (Performer i : errorTargets) {
			if (!i.getSimpleName().equals(ss[0]))
				continue;
			p = i;
		}

		if (ss.length == 1)
			return p;

		if (p == null)
			throw new RuntimeException("Componente n�o identificado: " + ss[0]);

		relName = relName.substring(ss[0].length() + 1);
		return p.findDeviceRel(relName);
	}

	@Override
	public String getSimpleName() {
		return getName();
	}

	@Override
	public String getFullName() {
		return getName();
	}

	public void showTree() {
		System.out.println("macro: " + getName());

		for (TargetPerformer t : initTargets) {
			System.out.println("init: ");
			t.showTree();
		}

		System.out.println("performer: ");
		performer.showTree();

		for (TargetPerformer t : finalTargets) {
			System.out.println("final: ");
			t.showTree();
		}
	}

//	private static String extractSimpleFileName(String a) {
//		File f = new File(a);
//		String r = f.getName();
//		if (r.toLowerCase().endsWith(".sql") || r.toLowerCase().endsWith(".dbs"))
//			r = r.substring(0, r.length() - 4);
//		return r;
//	}

	public DBS getProgram() {
		return program;
	}

	public void setProgram(DBS program) {
		this.program = program;
	}

	public void assignDataAsVars(DataSet dataSet) throws SQLException, IOException, ParseException {
		if (dataSet == null)
			return;

		String[] fields = dataSet.getFieldNames();
		while (dataSet.next()) {
			Record r = dataSet.currentRecord();
			for (String f : fields) {
				String v = r.getValueAsString(f);
				writeVar(f, v);
			}
		}
	}

	public void assignResultDataAsVars(Result r) throws SQLException, IOException, ParseException {
		if (r != null)
			assignDataAsVars(r.getDataSet());
	}

	@Override
	public String[] getSavePointColumns() {
		return null;
	}

	@Override
	public String getAlias() {
		return null;
	}



	public void addInitTarget(TargetPerformer t) {
		nameDevice("init", t, initTargets);
		initTargets.add(t);
	}

	public void addFinalTarget(TargetPerformer t) {
		nameDevice("final", t, finalTargets);
		finalTargets.add(t);
	}

	public void addErrorTarget(TargetPerformer t) {
		nameDevice("error", t, errorTargets);
		errorTargets.add(t);
	}

	@Override
	public String toString() {
		return "Macro [name=" + name + "]";
	}
}
