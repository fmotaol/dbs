package core.parsing;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import core.DBS;
import core.Device;
import core.Macro;
import core.events.ElseEvent;
import core.events.Event;
import core.events.IfEvent;
import core.join.MatchJoin;
import core.parsing.expression.Comparison;
import core.parsing.expression.Constant;
import core.parsing.expression.Expression;
import core.parsing.expression.In;
import core.performer.Performer;
import core.performer.SourcePerformer;
import core.performer.TargetPerformer;
import util.Strings;
import util.Util;
import util.logical.Assert;

public class CommandParser {

	private DBS program;

	private Macro currentEngine;

	private String content;

	private ArrayList<String> configVars = new ArrayList<String>();

	private HashMap<String, String> configVarValues = new HashMap<String, String>();

	private String hintName;

	public CommandParser(DBS program, String content, String hintName) {
		super();
		this.program = program;
		this.content = content;
		this.hintName = hintName;
	}

	private void parseRow(Macro engine, String row) throws ParseException {

		String originalRow = new String(row);

		if (row.startsWith("﻿"))
			row = row.substring(3);

		row = row.trim();

		if (row.startsWith("//")) {
			// comentário
			return;
		}

		if (row.startsWith("$") && !row.startsWith("$$")) {
			// if (row.startsWith("$")) {
			registerConfigVar(row);
			return;
		}

		if (row.startsWith("#")) {
			changeCommandParsingContext(row);
			return;
		}

		if (!row.isEmpty())
			if (currentContextType == null)
				throw new ParseException("Não foi identificado tipo de contexto para " + originalRow, 0);
		addToCurrentSQL(originalRow);
	}

	private void addToCurrentSQL(String originalRow) {
		currentSQL.append("    ");
		currentSQL.append(originalRow + "\n");
	}

//	private boolean isInsideSQLCommand() {
//		return currentSQL.length() > 0;
//	}

	private void registerConfigVar(String row) throws ParseException {

		if (row.startsWith("$"))
			row = row.substring(1);
		else
			throw new ParseException("Erro interno", 0);

		String[] ss = row.split("=", 2);
		if (ss.length != 2)
			throw new ParseException("Variável de configuração sem valor", 0);
		String var = ss[0].trim();
		configVars.add(var);
		String value = ss[1].trim();
		configVarValues.put(var, value);
	}

	private String currentContextType = null;
	private String currentContextAlias = null;
	private String currentConnection = null;
	private StringBuilder currentSQL = new StringBuilder();

	private Performer currentPerformer;

	private String currentContextParam;

	private String currentContextComplement;

	private void changeCommandParsingContext(String row) throws ParseException {

		String previousContextType = currentContextType;

		processPreviousContext();

		currentContextType = null;
		currentContextAlias = null;
		currentSQL = new StringBuilder();

//		final String CONTEXT_PARAM_SPLITTER = "\\s*(\\=|\\s)\\s*";
		final String CONTEXT_PARAM_SPLITTER = "\\s*\\=\\s*";

		String[] ss = row.trim().split(CONTEXT_PARAM_SPLITTER, 2);

		String macroName = hintName;

		if (ss.length > 1) {
			String param = ss[1].trim();
			setCurrentContextParam(param);

			if (row.trim().startsWith("#macro")) {
				macroName = param;
			}
		} /*
			 * else {
			 * 
			 * ss = row.split("\\s+");
			 * 
			 * if (ss.length > 1) { int i = row.indexOf(ss[1]); String param =
			 * row.substring(i); if (row.trim().startsWith("#macro")) { macroName = param; }
			 * setCurrentContextComplement(param.trim()); } else
			 * setCurrentContextComplement(null); }
			 */

		if (row.trim().startsWith("#macro")) {
			addNewEngine(macroName);
		} else {
			if (program.getEngines().length == 0)
				addNewEngine(macroName);
		}

		if (currentPerformer == null) {
			if (previousContextType == null || previousContextType.equals("macro")) {
				if (currentEngine != null && configVars.size() > 0) {
					for (String var : configVars) {
						String value = configVarValues.get(var);
						currentEngine.assignConfigVar(var, value);
					}
					configVars.clear();
					configVarValues.clear();
				}
			}
		}

		if (row.trim().startsWith("#")) {
			String context = row.trim().substring(1).split(CONTEXT_PARAM_SPLITTER)[0].trim();
			String[] ctxps = context.split("\\s+");
			if (ctxps.length < 1)
				throw new ParseException("Não foi identificado tipo de contexto", 0);
			currentContextType = ctxps[0];

			if (currentContextType.equals("source") || currentContextType.equals("target")) {

				if (ctxps.length == 2)
					currentContextAlias = ctxps[1];
				else
					currentContextAlias = null;

			} else {

				if (currentContextType.equalsIgnoreCase("if") || currentContextType.equalsIgnoreCase("else"))
					return;

				if (ctxps.length == 2)
					setCurrentContextComplement(ctxps[1]);
				else
					setCurrentContextComplement(null);

			}

			if (ctxps.length > 2)
				throw new ParseException("Sintaxe incorreta para declaração de contexto", 0);
		}

		if (ss.length > 1) {
//			if (!Event.isKnown(currentContext))
			setCurrentConnection();
		} else {
			currentConnection = null;
			setCurrentContextParam(null);
		}

	}

	private void setCurrentContextParam(String param) {

		currentContextParam = param;

	}

	private void setCurrentContextComplement(String complement) {

		currentContextComplement = complement;

	}

	private void setCurrentConnection() throws ParseException {
		if (Util.isAnyIgnoreCase(currentContextType, "init", "final", "error", "source", "target", "iffound",
				"ifnotfound") || Event.isKnown(currentContextType)) {
			if (currentContextParam.equals(Performer.PRIVATE_CONNECTION_ID))
				throw new ParseException("Identificador de conexão proibido: " + Performer.PRIVATE_CONNECTION_ID, 0);

			currentConnection = currentContextParam;
			return;
		}

	}

	private void processPreviousContext() throws ParseException {

		if (currentContextType == null) {
			return;
		}

		Performer previousPerformer = currentPerformer;
		boolean changePerformer = true;

		if (isPerformerContext(currentContextType)) {

			if (currentContextType.equalsIgnoreCase("init")) {

				assignInitTarget();

			} else if (currentContextType.equalsIgnoreCase("final")) {

				assignFinalTarget();

			} else if (currentContextType.equalsIgnoreCase("error")) {

				assignErrorTarget();

			} else if (currentContextType.equalsIgnoreCase("source")) {

				assignSource(true);

			} else if (currentContextType.equalsIgnoreCase("target")) {

				assignTarget(true);

			} else if (Event.isKnown(currentContextType)) {

				TargetPerformer t = assignTarget(true);
				t.assignEvent(currentContextType, currentContextParam);
				treatEvent(t, previousPerformer);

			} else if (currentContextType.equalsIgnoreCase("ifnotfound")) {

				Integer threshold = setupRecordsFoundThresholdForCompatibility();
				TargetPerformer t = assignTarget(previousPerformer, false);
				changePerformer = false;
				previousPerformer.setActionIfNotFound(t, threshold);

			} else if (currentContextType.equalsIgnoreCase("iffound")) {
				Integer threshold = setupRecordsFoundThresholdForCompatibility();
				TargetPerformer t = assignTarget(previousPerformer, false);
				changePerformer = false;
				previousPerformer.setActionIfFound(t, threshold);

			} else if (currentContextType.equalsIgnoreCase("iferror")) {

				String e = currentContextParam;
				if (e == null)
					e = currentContextComplement;

				if (e == null)
					e = "Exception";

				TargetPerformer t = assignTarget(previousPerformer, false);
				changePerformer = false;
				previousPerformer.setActionIfError(t, e);

			}

			assignConfigVars();
			if (!changePerformer)
				currentPerformer = previousPerformer;

			return;

		}

		if (currentContextType.equals("macro")) {
			return;
		}

		if (currentContextType.equals("\\")) {
			recoverPreviousPerformer();
			return;
		}

		if (currentContextType.equalsIgnoreCase("if")) {

			assignConditionalSQL(currentSQL, currentContextParam);

		} else if (currentContextType.equalsIgnoreCase("else")) {

			assignConditionalSQL(currentSQL, currentContextParam);

		} else
			throw new ParseException("Tipo de contexto desconhecido: " + currentContextType, 0);

	}

	private void assignConditionalSQL(StringBuilder sql, String param) throws ParseException {
		if (currentPerformer == null)
			throw new ParseException("Não foi identificado um performer para o SQL em " + currentContextType, 0);
		Expression<Boolean> exp = parseCondition(param);
		currentPerformer.addConditionalCommand(sql.toString(), exp);
	}

	public static Expression<Boolean> parseCondition(String param) throws ParseException {

		try {
			boolean value = parseBoolean(param);
			return new Constant<Boolean>(value);
		} catch (ParseException e) {
		}

		try {
			Expression<Boolean> r = parseComparison(param);
			return r;
		} catch (ParseException e) {
		}

		throw new ParseException("Condição não reconhecida: " + param, 0);
	}

	public static boolean parseBoolean(String value) throws ParseException {
		value = value.trim();
		if ("true".equalsIgnoreCase(value))
			return true;
		if ("false".equalsIgnoreCase(value))
			return false;

		throw new ParseException(value + " não é um booleano", 0);
	}

	private Integer setupRecordsFoundThresholdForCompatibility() {
		if (currentContextParam != null)
			if (Util.isInteger(currentContextParam)) {
				System.out.println("AVISO: parâmetro " + currentContextParam + " encontrado no lugar da conexão em "
						+ currentContextType);
				System.out.println(
						"       atribuído a FOUND_RECORDS_THRESHOLD para compatibilidade com versões anteriores");
				int r = Integer.parseInt(currentContextParam);
				currentConnection = null;
				return r;
			}
		return null;
	}

	private void treatEvent(TargetPerformer current, Performer previous) throws ParseException {
		if (current.getEvent() instanceof ElseEvent) {
			Event pe = previous.getEvent();
			if ((pe == null) || (!(pe instanceof IfEvent)))
				throw new ParseException("Evento 'else' deve acompanhar um 'if'", 0);
			ElseEvent ee = (ElseEvent) current.getEvent();
			ee.setIfEvent((IfEvent) pe);
		}
	}

	private void assignConfigVars() {
		if (configVars.size() > 0) {
			for (String var : configVars) {
				String value = configVarValues.get(var);
				currentPerformer.assignConfigVar(var, value);
			}
			configVars.clear();
			configVarValues.clear();
		}
	}

	private static String[] contextCommands = { "init", "final", "error", "source", "target", "iffound", "ifnotfound",
			"iferror", "match", "miss", "outer" };

	boolean isPerformerContext(String context) {
		if (Util.contains(contextCommands, context))
			return true;

		if (Event.isKnown(context))
			return true;

		return false;
	}

	private void recoverPreviousPerformer() {
		currentPerformer = currentPerformer.getInvoker();
	}

	private Macro currentEngine() {
		return currentEngine;
	}

	private TargetPerformer assignInitTarget() {
		String sql = currentSQLAsString();
		TargetPerformer t = new TargetPerformer(currentEngine(), currentConnection, null);
		t.setTemplateCommand(sql);
		currentEngine().addInitTarget(t);
		return t;
	}

	private TargetPerformer assignFinalTarget() {
		String sql = currentSQLAsString();
		TargetPerformer t = new TargetPerformer(currentEngine(), currentConnection, null);
		t.setTemplateCommand(sql);
		currentEngine().addFinalTarget(t);
		return t;
	}

	private TargetPerformer assignErrorTarget() {
		String sql = currentSQLAsString();
		TargetPerformer t = new TargetPerformer(currentEngine(), currentConnection, null);
		t.setTemplateCommand(sql);
		currentEngine().addErrorTarget(t);
		return t;
	}

	private String currentSQLAsString() {
		String s = currentSQL.toString();
		s = s.replaceAll("\\s+$", "");
		return s;
	}

	private TargetPerformer assignTarget(boolean setAsSlave) throws ParseException {
		return assignTarget(null, setAsSlave);
	}

	private TargetPerformer assignTarget(Performer invoker, boolean setAsSlave) throws ParseException {
		if (invoker == null)
			invoker = currentSource();
		String sql = currentSQLAsString();
		TargetPerformer t = new TargetPerformer(currentEngine(), currentConnection, invoker);
		t.setTemplateCommand(sql);
		t.setAlias(currentContextAlias);
		checkAliasDuplicity(currentContextAlias, t);
		addPerformer(t, invoker, setAsSlave);
		currentPerformer = t;
		return t;
	}

	private void checkAliasDuplicity(String alias, Performer p) throws ParseException {
		if (alias == null)
			return;
		Device d = program.findDeviceByAlias(alias);
		if ((d != null) && (d != p))
			throw new ParseException("Já existe outra seção declarada com o alias " + alias, 0);
	}

	private SourcePerformer assignSource(boolean setAsSlave) throws ParseException {
		SourcePerformer cs = currentSource();
		SourcePerformer s = new SourcePerformer(currentEngine(), currentConnection, cs);
		String sql = currentSQLAsString();
		s.setTemplateCommand(sql);
		s.setAlias(currentContextAlias);
		checkAliasDuplicity(currentContextAlias, s);
		addPerformer(s, cs, setAsSlave);
		currentPerformer = s;
		return s;
	}

	private void addPerformer(Performer performer, Performer invoker, boolean setAsSlave) throws ParseException {
//		SourcePerformer sourcePerformer = currentSource();

		if (invoker != null) {

			if (setAsSlave) {
				if (!(invoker instanceof SourcePerformer))
					throw new ParseException("Invoker não é um Source", 0);
				SourcePerformer sp = (SourcePerformer) invoker;
				sp.addSlave(performer);
			}

		} else
			currentEngine().setPerformer(performer);

	}

	private SourcePerformer currentSource() {
		SourcePerformer r = null;
		if (currentPerformer != null) {
			r = currentPerformer.nearestSource();
			// if (!(currentPerformer instanceof SourceQuery))
			// throw new RuntimeException("Invoker não é um Source");
		}

		// SourceQuery source = (SourceQuery) currentPerformer;
		if (currentPerformer == null) {
			currentEngine().setPerformer(r);
			currentPerformer = r;
		}

		return r;
	}

	public void parse() throws ParseException {
		String[] rows = content.split("\n");
		for (int i = 0; i < rows.length; i++) {
			String row = rows[i];
			if ((row != null) && (!row.trim().isEmpty()))
				parseRow(currentEngine(), row);
		}
		processPreviousContext();

	}

	private void addNewEngine(String macroName) {
		currentEngine = program.addNewEngine(macroName);
		currentPerformer = null;
	}

	public static String clearCommentedLines(String command) {
		while (command.startsWith("--")) {
			String[] ss = command.split("\n", 2);
			if (ss.length == 2)
				command = ss[1].trim();
			else
				command = "";
		}
		return command;
	}

	public static Expression<Boolean> parseComparison(String rawExp) throws ParseException {
		rawExp = rawExp.trim();
		final String split_regex = "\\s*(=|<>|<|<=|>|>=|(\\sis\\s)|(\\sin\\s)|(\\sIN\\s))\\s*";
		String[] ss = rawExp.split(split_regex, 2);
		if (ss.length != 2)
			throw new ParseException(rawExp + " não é uma comparação", 0);

		String op = rawExp.substring(ss[0].length(), rawExp.length() - ss[1].length());
		op = op.trim();

		if (op.equalsIgnoreCase("in")) {
			String[] list = parseParentizedList(ss[1]);
			return new In(ss[0], list);
		}
		Comparison r = new Comparison(ss[0], op, ss[1]);
		return r;
	}

	private static String[] parseParentizedList(String text) throws ParseException {
		text = Strings.removeStart(text, "(");
		text = Strings.removeEnd(text, ")");
		String[] r = Util.splitByComma(text, true);
		return r;
	}

	public static String assertAndRemoveFirst(String text, String part) throws ParseException {
		if (!text.startsWith(part))
			throw new ParseException(part + " esperado no início da sentença " + text, 0);
		text = text.substring(part.length());
		return text;
	}

	public static String assertAndRemoveLast(String text, String part) throws ParseException {
		if (!text.endsWith(part))
			throw new ParseException(part + " esperado no início da sentença " + text, 0);
		text = text.substring(0, text.length() - part.length());
		return text;
	}

	public static void setKeyDefinition(MatchJoin join, String declaration) {

		Assert.notNull(declaration);
		String[] ss = declaration.split("\\s*\\=\\s*"); // espaços e igual e espaços
		String[] upperKey;
		String[] lowerKey;

		if (ss.length == 2) {
//			TODO ainda não trata a ambiguidade de campos com mesmo nome, quem seria em upper e quem seria em lower 
			upperKey = ss[0].split(",");
			lowerKey = ss[1].split(",");
		} else if (ss.length == 1) { // permite só listar o(s) campo(s), quando os nomes deste(s) são iguais nas duas
			upperKey = ss[0].split(",");
			lowerKey = ss[0].split(",");
		} else
			throw new RuntimeException("Sintaxe incorreta na definição de JOIN_KEY");

		join.setKeyDefinition(upperKey, lowerKey);
	}

}
