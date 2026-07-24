package core.performer;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import core.DBS;
import core.dataset.CounterDataSet;
import core.dataset.DataSet;
import core.dataset.EnumDataSet;
import core.parsing.Parse;
import core.sql.Language;
import util.Util;

public class DefaultConnection extends DBSConnection {

	@Override
	public DataSet query(String sql, Performer invoker) {
		sql = sql.trim();
		if (Parse.match(sql, "^for\\s+"))
			return queryForLoop(sql, invoker);
		throw new RuntimeException("Comando desconhecido: " + sql);
	}

	@Override
	public void setAutoCommit(Boolean autoCommit) throws SQLException {
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public Result execute(String commands, Performer invoker, Context context) {
		if (commands == null)
			throw new RuntimeException("Ação nula");
		commands = commands.replace("\r", "");

		String[] rows = commands.split("\n");
		for (String row : rows) {
			String action = row.trim();
			if (action.startsWith("--") || action.startsWith("//"))
				continue;

			executeAction(action, invoker, context);
		}
		return new Result(0);
	}

	private void executeAction(String action, Performer invoker, Context context) {
		if (action == null)
			return;
		if (action.trim().equals(""))
			return;

		if (invoker == null)
			throw new RuntimeException("Invoker nulo");

		if (!parseDBSAction(invoker, action, true, context))
			throw new RuntimeException("[DBS DefaultConnection] Comando não reconhecido: " + action);
	}

	static boolean parseDBSAction(Performer invoker, String action, boolean perform, Context context) {
		if (Util.nullOrEmpty(action))
			return true;
		
		action = action.trim();
		if (action.endsWith(";"))
			action = action.substring(0, action.length() - 1);

		if (action.equalsIgnoreCase("repeat")) {
			if (perform)
				invoker.setRepeat(true);
			return true;
		}

		if (action.equalsIgnoreCase("stop")) {
			if (perform)
				invoker.setRepeat(false);
			return true;
		}

		if (action.equalsIgnoreCase("restart")) {
			if (perform)
				DBS.mainProgram.restart = true;
			return true;
		}

		if (action.equalsIgnoreCase("skip")) {
			return true;
		}

		if (action.toLowerCase().startsWith("error")) {
			raiseError(action);
		}
		
		if (action.equalsIgnoreCase("next")) {
			if (perform) {
				if (invoker == null)
					throw new RuntimeException("Invoker não localizado");
				if (!(invoker instanceof SourcePerformer))
					throw new RuntimeException("Invoker não é SourcePerformer");

				((SourcePerformer) invoker).jumpToNextRecord();
			}

			return true;
		}

		if (action.equalsIgnoreCase("reconnect and try again")) {
			if (perform) {
				if (invoker.isDynamicConnection())
					throw new RuntimeException("Reconexão automática não permitida para conexões dinâmicas");
				invoker.getConnection().reconnect();
				invoker.tryAgain = true;
			}
			return true;
		}

		if (action.toLowerCase().startsWith("save record as")) {
			invoker.executeSaveRecordAction(action, context, perform);
			return true;
		}

		if (action.toLowerCase().startsWith("set ")) {
			invoker.executeSetVarAction(action, context, perform);
			return true;
		}

		if (action.toLowerCase().startsWith("clear ")) {
			invoker.executeClearVarAction(action, perform);
			return true;
		}

		if (action.toLowerCase().startsWith("append ")) {
			invoker.executeAppendVarAction(action, context, perform);
			return true;
		}

		if (action.toLowerCase().startsWith("print ")) {
			printAction(action, invoker, context);
			return true;
		}
		
		if (action.toLowerCase().startsWith("dbs ")) {
			invoker.executeSubProgram(action.substring(4).trim(), context);
			return true;
		}

		if (action.toLowerCase().startsWith("exec ")) {
			invoker.callExternalProgram(action.substring(4).trim(), context);
			return true;
		}

		return false;
	}
	
	static void printAction(String cmd, Performer invoker, Context context) {
		cmd = cmd.substring(6, cmd.length() - 1);
		cmd = invoker.defaultConcretizer.concretizeAll(cmd, context);
		System.out.println(cmd);
	}

	static void raiseError(String cmd) {
		cmd = cmd.substring(5, cmd.length());
		throw new RuntimeException(cmd);
	}

	@Override
	public Batch createBatch(Performer performer) {
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void defaultStartImportingData(TargetPerformer target) {
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void defaultImportRow(TargetPerformer target, Context context) {
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void defaultEndImportingData(TargetPerformer target) {
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void reconnect() {
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void close() {
		// nada
	}

//	static String ENUM_ITEM_REGEX = ParseUtil.REGEX_INT + "|" + "\\'.*\\'";
	static String ENUM_ITEM_REGEX = "[^\\},\\s]+";

//	static String FOR_REGEX = "^for\\s+" // 1
//			+ "(?<var>" + ParseUtil.REGEX_ID + ")" // 2
//			+ "\\s+in\\s+(?<loop>(?<start>" + ParseUtil.REGEX_INT + ")" // 3
//			+ "\\s*\\.\\.\\s*(?<end>" + ParseUtil.REGEX_INT + ")" // 4
//			+ "(\\s+step\\s+(?<step>" + ParseUtil.REGEX_INT + "))?" // 5
//			+ "\\s*)|" // 6
//			+ "(?<enum>\\{(?<items>\\s*" + ENUM_ITEM_REGEX + "\\s*(,\\s*" + ENUM_ITEM_REGEX + "\\s*)*)?\\})$";

	static String FOR_LOOP_REGEX = "^for\\s+" // 1
			+ "(?<var>" + Parse.REGEX_ID + ")" // 2
			+ "\\s+in\\s+(?<loop>(?<start>" + Parse.REGEX_INT + ")" // 3
			+ "\\s*\\.\\.\\s*(?<end>" + Parse.REGEX_INT + ")" // 4
			+ "(\\s+step\\s+(?<step>" + Parse.REGEX_INT + "))?" // 5
			+ "\\s*)$"; // 6

	static String FOR_ENUM_REGEX = "^for\\s+" // 1
			+ "(?<var>" + Parse.REGEX_ID + ")" // 2
			+ "\\s+in\\s+(?<enum>\\{(?<items>\\s*" + ENUM_ITEM_REGEX + "\\s*(,\\s*" + ENUM_ITEM_REGEX + "\\s*)*)?\\})$";

	static Pattern FOR_LOOP_PATTERN = Pattern.compile(FOR_LOOP_REGEX,
			Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	static Pattern FOR_ENUM_PATTERN = Pattern.compile(FOR_ENUM_REGEX,
			Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	private DataSet queryForLoop(String sql, Performer invoker) {
		// for localidade in 1 .. 1000

		Matcher matcher = FOR_LOOP_PATTERN.matcher(sql);

		if (matcher.matches()) {

			String var = matcher.group("var");

			if (matcher.group("loop") != null) {

				String start = matcher.group("start");
				String end = matcher.group("end");
				String step = matcher.group("step");
//				StringConcretizer.replaceFirst(sql, group, exp);

				DataSet r = newCounterDataSet(var, start, end, step);
				return r;
			}

		}

		matcher = FOR_ENUM_PATTERN.matcher(sql);

		if (matcher.matches()) {
			if (matcher.group("enum") != null) {

				String var = matcher.group("var");
				String items = matcher.group("items");
				if (items == null)
					throw new RuntimeException("Conjunto vazio");
				String[] is = items.split(",");
				is = Util.trim(is);

				DataSet r = newEnumDataSet(var, is);
				return r;
			}
		}

		throw new RuntimeException("Tipo de loop for não suportado");

	}

	private <T> DataSet newEnumDataSet(String fieldName, T[] items) {
		return new EnumDataSet<T>(fieldName, items);
	}

	private CounterDataSet newCounterDataSet(String var, String start, String end, String step) {
		CounterDataSet r;
		int istart = Integer.parseInt(start);
		int iend = Integer.parseInt(end);
		r = new CounterDataSet(var, istart, iend);
		if (step != null) {
			int istep = Integer.parseInt(start);
			r.setStep(istep);
		}
		return r;
	}

	@Override
	public Language getLanguage() {
		return Language.defaultLanguage();
	}

	@Override
	public String getId() {
		return "default";
	}

}
