package core.parsing.replace;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import core.DBS;
import core.Macro;
import core.args.Argument;
import core.args.UndefinedArgAction;
import core.dataset.Field;
import core.dataset.FieldValueSource;
import core.dataset.Record;
import core.file.FileDataSet;
import core.file.FileListDataSet;
import core.parsing.Concretizer;
import core.parsing.string.Block;
import core.parsing.string.TransactionBlock;
import core.parsing.string.TransactionBlock.Transaction;
import core.performer.Context;
import core.performer.Performer;
import core.performer.TargetPerformer;
import core.sql.DefaultLanguage;
import core.sql.Language;
import util.Getable;
import util.Util;
import util.logical.Check;

public class StringConcretizer extends Concretizer {

	public boolean cascadeReference = true; // TODO não está compatível com a DOC

	private static final String DEFAULT_REF_SYMBOL = "@";

	private String refSymbol = DEFAULT_REF_SYMBOL;
	private String replPrefix = "";
//	private boolean concretizePostFix = false;

	public boolean forSQL = true;

	public boolean parseControlChars = false;

	private DBS program;

	private Macro engine;

	private Language language;

	private SubBlockConcretizer subBlock = new SubBlockConcretizer(this);

//	public boolean createArgs = false;

	public void setLanguage(Language language) {
		this.language = language;
	}

	public StringConcretizer(DBS program) {
		super(null);
		this.program = program;
		this.forSQL = false;
	}

	public StringConcretizer(Macro engine, Performer performer, boolean forSQL) {
		super(performer);
		this.program = engine.getProgram();
		this.engine = engine;
		this.forSQL = forSQL;
	}

	public Language getLanguage() {
		if (language == null)
			language = new DefaultLanguage();
		return language;
	}

	private void concretizeFieldsByAliasAndIndex(Block sql, String prefix, Record record, Context context) {
		if (record == null)
			return;
		String alias = record.getAlias();
		if (alias == null)
			return;
		concretizeFieldsByIndex(sql, prefix + alias + ".", record, context);
	}

	private void concretizeFieldsByIndex(Block sql, String prefix, Record record, Context context) {
		if (record == null)
			return;

		Field[] fields = record.getFields();

		for (int i = fields.length; i >= 1; i--) {

			if (!containsIgnoreCase(sql, prefix))
				return;

			String ref = prefix + i;
			if (containsIgnoreCase(sql, ref))
				concretizeRecordField(sql, prefix, fields[i - 1], ref, record);

			if (containsIgnoreCase(sql, prefix + "field[")) {
				ref = prefix + "field[" + i + "]";
				concretizeRecordField(sql, prefix, fields[i - 1], ref, record);
			}
		}
	}

	private void concretizeCrossFieldsAll(Block sql, String prefix, Record record) {
		if (performer == null)
			return;
		if (record == null)
			return;

		Field[] fs;
		Context context = new Context(null, record, null);
		String[] fieldsToFilter = performer.getFieldsToFilter(context);

		String ref = prefix + "*-pk";
		if (containsIgnoreCase(sql, ref)) {

			if (performer == null)
				throw new RuntimeException(
						"Não é possível inferir a chave primária da tabela porque existe performer associado");

			String[] pkFields = getPrimaryKeyFields(context);

			fs = context.record.getFields();

			fs = Field.removeByFieldName(fs, pkFields);
			String values = getLanguage().sqlValueList(fs, ", ", record, fieldsToFilter);
//			record.sqlValueList(fs, ", ", language, fieldsToFilter);
			replaceIgnoreCase(sql, ref, values);
		}

		ref = prefix + "*";
		if (contains(sql, ref)) {
			fs = record.getFields();
			// FIXME, Bruno - alteração realizada em 28 de julho de 2017
			Arrays.sort(fs);
			String values = getLanguage().sqlValueList(fs, ", ", record, fieldsToFilter);
//			String values = record.sqlValueList(fs, ", ", language, fieldsToFilter);
			if (contains(sql, ref)) {
				replaceIgnoreCase(sql, ref, values);
			}
		}

	}

	private String[] getPrimaryKeyFields(Context context) {
		return getPrimaryKeyFields(performer, context);
	}

	private static String[] getPrimaryKeyFields(Performer performer, Context context) {
		if (!(performer instanceof TargetPerformer))
			throw new RuntimeException("Impossível obter chave primária a não ser de um target");

		TargetPerformer t = (TargetPerformer) performer;
		String[] pkFields = t.getPrimaryKeyFields(context);
		return pkFields;
	}

	static boolean containsIgnoreCase(Block sb, String s) {
		boolean r = sb.contains(s, true);
		return r;
	}

	private void concretizeInvokerData(Block sql, Context context) {
		if (performer == null)
			return;
		if (context == null)
			return;
		if (context.record == null)
			return;

		concretizeFieldsByName(sql, refSymbol, context);
		concretizeFieldsByIndex(sql, refSymbol, context.record, context);
		concretizeFieldsByAliasAndIndex(sql, refSymbol, context.record, context);
		concretizeDataSetAttributes(sql, context);
		concretizeRecordAttributes(sql, context.record);

		concretizeContextData(sql, performer, context, "@#");
		concretizeContextData(sql, performer, context, refSymbol);

		concretizeConnectionAttributes(sql, performer, "@#");
		concretizeConnectionAttributes(sql, performer, refSymbol);
		concretizePrimaryKey(sql, refSymbol, context);
		concretizeCrossFieldsAssignments(sql, refSymbol, context);
		concretizeCrossFieldsAssignmentsWithAlias(sql, refSymbol, context);
		concretizeCrossFieldsAll(sql, refSymbol, context.record);
		concretizeCrossFieldsAllWithAlias(sql, refSymbol, context);

	}

	private void concretizeContextData(Block sql, Performer performer, Context context, String prefix) {
		if (performer == null)
			throw new RuntimeException("Bloco de execução não encontrado");

		if (!contains(sql, prefix))
			return;

		if (containsIgnoreCase(sql, prefix + "sql")) {
			String isql = context.dataSet.getInvokerData().getCommand();
			replaceIgnoreCase(sql, prefix + "sql", isql);
		}

		if (containsIgnoreCase(sql, prefix + "command")) {
			String isql = context.dataSet.getInvokerData().getCommand();
			replaceIgnoreCase(sql, prefix + "command", isql);
		}

		if (containsIgnoreCase(sql, prefix + "errorclass")) {
			Throwable e = context.getException();
			replaceIgnoreCase(sql, prefix + "errorclass", e.getClass().getName());
		}

		if (containsIgnoreCase(sql, prefix + "errormsg")) {
			Throwable e = context.getException();
			replaceIgnoreCase(sql, prefix + "errormsg", e.getMessage());
		}
	}

	private void concretizeConnectionAttributes(Block sql, Performer performer, String prefix) {
		if (performer == null)
			return;

		if (!contains(sql, prefix))
			return;

		if (containsIgnoreCase(sql, prefix + "connectionfile")) {
			throw new RuntimeException("ainda não implementado");
		}

		if (containsIgnoreCase(sql, prefix + "connectionurl")) {
			throw new RuntimeException("ainda não implementado");
		}

		if (containsIgnoreCase(sql, prefix + "connectionport")) {
			throw new RuntimeException("ainda não implementado");
		}

		if (containsIgnoreCase(sql, prefix + "connectionuser")) {
			throw new RuntimeException("ainda não implementado");
		}

		if (containsIgnoreCase(sql, prefix + "connectionuser")) {
			throw new RuntimeException("ainda não implementado");
		}

	}

	private void concretizeDataSetAttributes(Block sql, Context context) {
		concretizeDataSetAttributes(sql, context, "@#");
		concretizeDataSetAttributes(sql, context, refSymbol);
	}

	private void concretizeDataSetAttributes(Block sql, Context context, String prefix) {
		if (context == null)
			return;
		if (context.dataSet == null)
			return;

		if (!containsIgnoreCase(sql, prefix))
			return;

		if (containsIgnoreCase(sql, prefix + "execution")) {
			if (performer == null)
				return;
			int exec = performer.getExecution();
			replaceIgnoreCase(sql, prefix + "execution", exec + "");
		}

		if (containsIgnoreCase(sql, prefix + "fieldcount")) {
			int total = context.dataSet.getFieldCount();
			replaceIgnoreCase(sql, prefix + "fieldcount", total + "");
		}

		if (containsIgnoreCase(sql, prefix + "fieldname[")) {
			int total = context.dataSet.getFieldCount();
			for (int i = 1; i <= total; i++) {
				if (containsIgnoreCase(sql, prefix + "fieldname[" + i + "]")) {
					String name = context.dataSet.getFieldName(i);
					replaceIgnoreCase(sql, prefix + "fieldname[" + i + "]", includePrefix(replPrefix, name));
				}

			}
		}

		if (containsIgnoreCase(sql, prefix + "fieldnames")) {
			if (performer == null)
				return;
			String[] fieldsToFilter = performer.getFieldsToFilter(context);
			int total = context.dataSet.getFieldCount();
			Block sb = newStringBuilder();
			for (int i = 1; i <= total; i++) {
				String name = context.dataSet.getFieldName(i);
				if (containsIgnoreCase(fieldsToFilter, name))
					continue;
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(name);
			}

			replaceIgnoreCase(sql, prefix + "fieldnames", sb.toString());
			sb.setLength(0);
			sb.trimToSize();
		}

	}

	private String includePrefix(String replPrefix, String text) {
		if (!replPrefix.equals(DEFAULT_REF_SYMBOL))
			return replPrefix + text;
		if (!text.contains(","))
			return replPrefix + text;

		String[] list = text.split("\\s*,\\s*");
		String r = "";
		for (String s : list) {
			if (!"".equals(r))
				r += ", ";
			r += replPrefix + s;
		}
		return r;
	}

	private void concretizeRecordAttributes(Block sql, Record record) {
		concretizeRecordAttributes(sql, record, "@#");
		concretizeRecordAttributes(sql, record, refSymbol);
	}

	private void concretizeRecordAttributes(Block sql, Record record, String prefix) {
		if (record == null)
			return;

		if (containsIgnoreCase(sql, prefix + "rowid")) {
			int rowId = record.getRowId();
			replaceIgnoreCase(sql, prefix + "rowid", rowId + "");
		}
	}

	private void concretizeFieldsByName(Block sql, String prefix, Context context) {
		if (context == null)
			return;
		if (context.record == null)
			return;
		// mapeia sem as tags: @campo1

		concretizeFieldsByNameNoAlias(sql, prefix, context);
		concretizeFieldsByAliasAndName(sql, prefix, context);

	}

	private void concretizeFieldsByAliasAndName(Block sql, String prefix, Context context) {
		if (context == null)
			return;
		if (context.record == null)
			return;
		do {
			if (!sql.contains(refSymbol))
				break;

			concretizeFieldsByAliasAndName(sql, prefix, context.record, context);

			context = context.parent;
		} while (context != null);
	}

	private void concretizeFieldsByNameNoAlias(Block sql, String prefix, Context context) {
		if (context == null)
			return;
		if (context.record == null)
			return;
		Field[] fields = context.getDeepFields(true);
		for (Field field : fields)
			concretizeField(sql, prefix, field, field.getName(), context);
	}

	private void concretizeFieldsByAliasAndName(Block sql, String prefix, Record record, Context context) {
		if (context == null)
			return;

		if (record.getRowId() < 0)
			return;

		String alias = record.getAlias();
		if (alias != null) {
			prefix = prefix + alias + ".";
			if (!sql.contains(prefix))
				return;

			Field[] fields = record.getFields();
			for (Field field : fields) {
				concretizeRecordField(sql, prefix, field, field.getName(), record);
			}
		}

	}

	private boolean containsIgnoreCase(String[] list, String string) {
		if (list == null)
			return false;
		return Util.containsIgnoreCase(list, string);
	}

	private void concretizeRecordField(Block sql, String prefix, Field field, String fieldRef,
			Record record) {
		if (!contains(sql, prefix))
			return;
		if (record == null)
			return;

		concretizeWithPreAndPostFix(sql, record, field, fieldRef, prefix, "");
	}

	private void concretizeField(Block sql, String prefix, Field field, String fieldRef, Context context) {
		if (!contains(sql, prefix))
			return;
		if (context == null)
			return;

		concretizeWithPreAndPostFix(sql, context, field, fieldRef, prefix, "");
	}

	private void concretizeFieldWithLevel(Block sql, String prefix, Field field, String fieldRef,
			Record record, Context context) {
		int level = context.getLevelOf(record);
		String postfix = "\\{" + level + "\\}";
		concretizeWithPreAndPostFix(sql, record, field, fieldRef, prefix, postfix);
	}

	private void concretizeWithPreAndPostFix(Block sql, FieldValueSource source, Field field,
			String fieldRef, String prefix, String postfix) {

		String fm = prefix + fieldRef + postfix;

		String s = fm + "::native";
		if (containsIgnoreCase(sql, s)) {
			String value = source.valueAsNative(field, getLanguage());
			replaceReference(sql, s, value);
		}

		s = fm + "::.";
		if (containsIgnoreCase(sql, s)) {
			String value = source.valueAsNative(field, getLanguage());
			replaceReference(sql, s, value);
		}

		s = fm + "::text";
		if (containsIgnoreCase(sql, s)) {
			String value = source.valueAsSQL(field, getLanguage());
			replaceReference(sql, s, value);
		}

		if (containsIgnoreCase(sql, fm)) {
			String sv;
			if (forSQL)
				sv = source.valueAsSQL(field, getLanguage());
			else
				sv = source.valueAsNative(field, getLanguage());
			replaceReference(sql, fm, sv);
		}
	}

	private void replaceReference(Block sql, String s, String value) {
		replaceIgnoreCase(sql, s, includePrefix(replPrefix, value));
	}

	private void concretizeCrossFieldsAssignmentsWithAlias(Block sql, String prefix, Context context) {
		if (context == null)
			return;
		if (context.record == null)
			return;

		do {
			String alias = context.record.getAlias();
			if (alias != null) {
				concretizeCrossFieldsAssignments(sql, alias + ".", context);
			}
			context = context.getParent();
		} while (context != null);
	}

	private void concretizeCrossFieldsAllWithAlias(Block sql, String prefix, Context context) {

		if (context == null)
			return;
		if (context.record == null)
			return;

		do {
			String alias = context.record.getAlias();
			if (alias != null) {
				concretizeCrossFieldsAll(sql, prefix + alias + ".", context.record);
			}
			context = context.getParent();
		} while (context != null);
	}

	private void concretizeCrossFieldsAssignments(Block sql, String prefix, Context context) {
		if (context == null)
			return;
		if (context.record == null)
			return;

		Field[] fields;

		String[] fieldsToFilter = performer.getFieldsToFilter(context);

		String ref = prefix + "*=and";
		if (containsIgnoreCase(sql, ref)) {

			fields = context.record.getFields();
			String values = sqlAssignmentList(context, fields, fieldsToFilter);

			if (containsIgnoreCase(sql, ref)) {
				replaceIgnoreCase(sql, ref, values);
				// record.addAllInto(usedFields, fieldsToFilter);
			}
		}

		if (containsIgnoreCase(sql, "@*=-pk")) {

			fields = context.record.getFields();
			if (performer == null)
				throw new RuntimeException(
						"Não é possível inferir a chave primária da tabela porque existe performer associado");
			String[] pkFields = getPrimaryKeyFields(context);
			fields = Field.removeByFieldName(fields, pkFields);
			String values = sqlAssignmentList(context, fields, " = ", ", ", fieldsToFilter);

			replaceIgnoreCase(sql, "@*=-pk", values);
		}

		if (contains(sql, "@*=")) {

			fields = context.record.getFields();
			String values = sqlAssignmentList(context, fields, " = ", ", ", fieldsToFilter);

			replaceIgnoreCase(sql, "@*=", values);
			// record.addAllInto(usedFields, fieldsToFilter);

		}
	}

	private boolean contains(Block sb, String s) {
		return sb.contains(s);
		// return contains(sb.getOriginalBuilder(), s);
	}

	private static void replaceIgnoreCase(Block sb, String from, String to) {
		sb.replace(from, to, true, false);
	}

	private void concretizeFromSavedRecords(Block sql, Context context) {
		if (context == null)
			return;
		if (context.dataSet == null)
			return;

		// if (!currentRecordName.equalsIgnoreCase("current"))
		// return;

		if (contains(sql, "@[")) {
//			if (source == null)
//				throw new RuntimeException("Não foi informado o DataSet - impossível recuperar registros salvos");

			for (String name : context.getSavedRecordNames(true)) {
				Record record = context.getRecord(name);
				Context c = new Context(context.parent, record, context.dataSet);
				concretizeFromSavedRecord(sql, c, name);
			}
		}
	}

	private void concretizeFromSavedRecord(Block sql, Context context, String recordName) {
		if (context == null)
			return;

		String prefix = "@[" + recordName + "]";
		if (containsIgnoreCase(sql, prefix)) {

			concretizePrimaryKey(sql, prefix, context);
			concretizeCrossFieldsAssignments(sql, prefix, context);
			concretizeCrossFieldsAll(sql, prefix, context.record);
			concretizeFieldsByName(sql, prefix, context);
			concretizeFieldsByIndex(sql, prefix, context.record, context);

		}
	}

	private void concretizeRef(Block sql, String ref, Getable<String> g) {
		if (containsIgnoreCase(sql, ref)) {
			String value = g.get();
			replaceIgnoreCase(sql, ref, value);
		}
	}

	private void concretizeVariables(Block sql, Macro engine) {
		if (engine == null)
			return;

		if (!contains(sql, refSymbol))
			return;

		for (String var : engine.getVarNames()) { // @TODO sort by varname length
			if (containsIgnoreCase(sql, "::text")) {
				String tvar = var + "::text";
				concretizeRef(sql, "@#" + tvar, () -> getLanguage().stringValueAsSQL(engine.readVar(var)));
				concretizeRef(sql, refSymbol + tvar, () -> getLanguage().stringValueAsSQL(engine.readVar(var)));
			}

			concretizeRef(sql, "@#" + var, () -> engine.readVar(var));
			concretizeRef(sql, refSymbol + var, () -> engine.readVar(var));

		}
	}

	private void concretizeSystemVars(Block sql, Macro engine) {
		if (engine == null)
			return;

		if (!contains(sql, refSymbol))
			return;

		for (String var : engine.getSystemVars()) { // @TODO sort by varname length

			String svar = refSymbol + var;
			String scvar = "@#" + var;

			if (!containsIgnoreCase(sql, svar) && !containsIgnoreCase(sql, scvar))
				continue;

			if (containsIgnoreCase(sql, var + "::text")) {
				concretizeRef(sql, scvar + "::text",
						() -> getLanguage().stringValueAsSQL(engine.readSystemVar(var) + ""));
				concretizeRef(sql, svar + "::text",
						() -> getLanguage().stringValueAsSQL(engine.readSystemVar(var) + ""));
			}

			concretizeRef(sql, scvar, () -> getLanguage().valueAsSQL(engine.readSystemVar(var)));
			concretizeRef(sql, svar, () -> getLanguage().valueAsSQL(engine.readSystemVar(var)));

		}
	}

	private void concretizePrimaryKey(Block sql, String prefix, Context context) {
		if (performer == null)
			return;
		if (context == null)
			return;

		String s = prefix + "*=pk";
		if (containsIgnoreCase(sql, s)) {

//			if (performer == null)
//				throw new RuntimeException(
//						"Não é possível inferir a chave primária da tabela porque existe performer associado");

			String pkc = getConcretePrimaryKeyCondition(sql.toString(), context);
			replaceIgnoreCase(sql, s, pkc);

		}

	}

	private String getConcretePrimaryKeyCondition(String sql, Context context) {
//		Record record = null;
//		if (context != null)
//			record = context.record;
		String pkc = getPrimaryKeyConditionAsAssignment(sql, performer, context);
		return pkc;
	}

	private void concretizeAll(Block sql, Context context) {

		subBlock.scrambleLiteralBlocks(sql);

		Transaction t = sql.newTransaction((s) -> s.contains("@"));
		concretizeFileName(sql, context);
		concretizeSubBlocks(sql, performer, context);
		concretizeFromSavedRecords(sql, context);
		concretizeReferences(sql, context, Argument.undefinedAction);
		if (performer != null)
			if (performer.clearUnknownVarReferences())
				clearUnkownVarReferences(sql);
		if (t != null)
			t.commit();

		subBlock.unscrambleLiteralBlocks(sql);
	}

	private void concretizeSubBlocks(Block sql, Performer performer, Context context) {
		if (!concretizeSubBlocks)
			return;
		subBlock.concretizeBlocks(sql, performer, context);
	}

	private void concretizeFileName(Block sql, Context context) {
		if (context == null)
			return;
		if (context.dataSet == null)
			return;

		String refFileName = refSymbol + "filename";
		if (containsIgnoreCase(sql, refFileName)) {
			if (context.dataSet instanceof FileListDataSet)
				replaceIgnoreCase(sql, refFileName, context.dataSet.readValue(context.dataSet.getRowId()).toString());
			if (context.dataSet instanceof FileDataSet)
				replaceIgnoreCase(sql, refFileName, ((FileDataSet) context.dataSet).getFilePath());
		}
	}

	void concretizeReferences(Block sql, Context context, UndefinedArgAction undefinedArgAction) {
		concretizeInvokerData(sql, context);
		concretizeTableAttributes(sql, context);
		concretizeArgs(sql, undefinedArgAction);
		concretizeVariables(sql, engine);
		concretizeSystemVars(sql, engine);
	}

	private void concretizeTableAttributes(Block sql, Context context) {
		if (performer == null)
			return;

		if (containsIgnoreCase(sql, "@#tablename")) {
			TargetPerformer target = getTargetPerformer();
			replaceIgnoreCase(sql, "@#tablename", target.getTableName(context));
		}

		if (containsIgnoreCase(sql, "@#tablefields")) {
			TargetPerformer target = getTargetPerformer();
			String[] fs = target.getTableFields(context);
			String s = Util.concat(fs, ", ");
			replaceIgnoreCase(sql, "@#tablefields", s);
		}

		if (containsIgnoreCase(sql, "@#tablepk")) {
			TargetPerformer target = getTargetPerformer();
			String[] fs = target.getPrimaryKeyFields(context);
			String s = Util.concat(fs, ", ");
			replaceIgnoreCase(sql, "@#tablepk", s);
		}
	}

	private TargetPerformer getTargetPerformer() {
		if (!(performer instanceof TargetPerformer))
			throw new RuntimeException("Contexto usando @#tablename não é um target");

		TargetPerformer target = (TargetPerformer) performer;
		return target;
	}

	public static final String regexVar = "(?<var>" + Pattern.quote("@#") + Util.regexIdentifier + ")";

	private void clearUnkownVarReferences(Block sql) {
		if (containsRegex(sql, regexVar))
			replaceRegex(sql, regexVar, "");
	}

	private static void replaceRegex(Block sb, String regex, String replacement) {
		// TODO avaliar performance
		Util.replaceRegex(sb.getOriginalBuilder(), regex, replacement);
	}

	private static boolean containsRegex(Block sb, String regex) {
		// TODO avaliar performance
		return Util.containsRegex(sb.getOriginalBuilder(), regex);
	}

	private void concretizeArgs(Block sql, UndefinedArgAction action) {

		if (!containsIgnoreCase(sql, "@arg"))
			return;

		concretizeArgsByIndex(sql, action);

		if (!containsIgnoreCase(sql, "@arg"))
			return;

		concretizeArgsByName(sql, action);

	}

	public static String extractArgName(String argPlaceHolder) {
		if (argPlaceHolder == null || argPlaceHolder.isEmpty()) {
			return null;
		}

		// Esta regex captura qualquer coisa entre [ e ]
		Pattern pattern = Pattern.compile("@arg\\[([^\\]]+)\\]");
		Matcher matcher = pattern.matcher(argPlaceHolder);

		if (matcher.find()) {
			return matcher.group(1);
		}

		throw new RuntimeException("Argumento mal-formado: " + argPlaceHolder);
	}

	private void concretizeArgsByName(Block sql, UndefinedArgAction action) {
		final String regex = "@arg\\[[a-zA-Z0-9._ ]+\\]";
		String[] as = sql.find(regex);
		for (String placeHolder : as) {
			String name = extractArgName(placeHolder);
			Argument a = argForConcretization(name, action);
			concretizeArgByName(sql, action, a);
		}

	}

	private void concretizeArgByName(Block sql, UndefinedArgAction action, Argument arg) {
		if (arg == null)
			return; // foi ignorado

		String ref = "@arg[" + arg.getName() + "]";
		if (!containsIgnoreCase(sql, ref))
			return;
		if (arg.shouldIgnore())
			return;

		String v = Check.coalesce(arg.getValue(action), "null");
		replaceIgnoreCase(sql, ref, v);
	}

	private void concretizeArgsByIndex(Block sql, UndefinedArgAction action) {
		for (int i = 1; i <= 50; i++) {
			if (!containsIgnoreCase(sql, "@arg")) // evita ir até o fim do loop desnecessariamente
				return;

			if (containsIgnoreCase(sql, "@arg[" + i + "]")) {
				Argument a = argForConcretization(i + "", action);
				concretizeArgByIndex(sql, i, action, a);
			}
		}
	}

	private void concretizeArgByIndex(Block sql, int index, UndefinedArgAction action, Argument arg) {
		if (arg == null)
			return; // foi ignorado

		String s = "@arg[" + index + "]";

		if (!containsIgnoreCase(sql, s))
			return;

		if (arg.shouldIgnore())
			return;

		String value = Check.coalesce(arg.getValue(action), "null");
		replaceIgnoreCase(sql, s, value);
	}

	public Argument argForConcretization(String name, UndefinedArgAction action) {
		return UndefinedArgAction.argumentFromAction(program, name, action);
	}

	public String concretizeAll(String text, Context context) {
		if (text == null)
			return null;
//			throw new RuntimeException("SQL ou texto nulo");

		if (!text.contains(refSymbol))
			return text;

		String s = text;
		String prev;

		String doubleRef = refSymbol + refSymbol;
		if (cascadeReference && s.contains(doubleRef)) {
			do {
				prev = s;
				StringConcretizer c = cascadeConcretizer(doubleRef);
				s = c.concretizeAll(s, context);
			} while (!prev.equals(s));
		}

		do {
			prev = s;
			Block sb = newStringBuilder(s);
			concretizeAll(sb, context);
			s = sb.toString();
			sb.setLength(0);
			sb.trimToSize();
		} while (recursiveReference && !prev.equals(s));

		if (parseControlChars)
			s = parseControlChars(s);

		return s;
	}

	private StringConcretizer cascadeConcretizer = null;

	private boolean concretizeSubBlocks = true;

	private StringConcretizer cascadeConcretizer(String refSymbol) {
		if (cascadeConcretizer == null)
			cascadeConcretizer = createCascadeConcretizer(refSymbol);
		return cascadeConcretizer;
	}

	private StringConcretizer createCascadeConcretizer(String refSymbol) {
		StringConcretizer r = new StringConcretizer(engine, performer, false);
		r.refSymbol = refSymbol;
		r.replPrefix = this.refSymbol;
		r.concretizeSubBlocks = false;
		return r;
	}

	private String parseControlChars(String s) {
		s = s.replace("\\n", "\n");
		s = s.replace("\\t", "\t");
//		s = s.replace("\\s", "\s");
		s = s.replace("\\b", "\b");
		s = s.replace("\\f", "\f");
		return s;
	}

	public String concretizeArgs(String text) {
		if (text == null)
			throw new RuntimeException("SQL ou texto nulo");

		if (!text.contains("@"))
			return text;

		String s = text;

		Block sb = newStringBuilder(s);
		concretizeArgs(sb, Argument.undefinedAction);
		s = sb.toString();
		sb.setLength(0);
		sb.trimToSize();

		return s;
	}

	String getFieldValue(Record record, Field field, boolean isNative, boolean forSQL) {
		String r;

		if (record != null) {
			r = record.valueAsString(field, getLanguage(), forSQL);
		} else {
			if (forSQL)
				r = "null";
			else
				r = "";
		}

		return r;
	}

	boolean concretizeAsBoolean(String booleanRef, Context context) {
		Block sb = newStringBuilder(booleanRef);
		concretizeReferences(sb, context, Argument.undefinedAction);
		Boolean b = parseAsBoolean(sb);
		return b;
	}

	Block newStringBuilder(String s) {
		return new TransactionBlock(s);
	}

	Block newStringBuilder() {
		return new TransactionBlock();
	}

	private Boolean parseAsBoolean(Block sb) {
		String s = sb.toString();
		if (s.equals("null"))
			return null;
		return Boolean.parseBoolean(s);
	}

	public static void checkVarNameSyntax(String name) {
		if (!isVarNameSyntax(name))
			throw new RuntimeException("Sintaxe inválida para identificador ou nome de variável");
	}

	public static boolean isVarNameSyntax(String name) {
		name.replace(Util.regexIdentifier, "");
		return (name.length() == 0);
	}

	public String sqlAssignmentList(Context context, Field[] fields, String operator, String separator,
			String[] fieldsToFilter) {
		StringBuilder r = new StringBuilder();
		for (Field f : fields) {
			if (fieldsToFilter != null)
				if (!Util.containsIgnoreCase(fieldsToFilter, f.getName()))
					continue;

			String sv = context.valueAsSQL(f, getLanguage());

			if (r.length() > 0)
				r.append(separator);

			Util.append(r, f.getName(), operator, sv);
		}
		String s = r.toString();
		r.setLength(0);
		r.trimToSize();
		return s;
	}

//	@Deprecated
//	protected static String nativeValueList(Record record, String[] fields, String separator) {
//		StringBuilder r = new StringBuilder();
//		for (String f : fields) {
//			Object value = record.getValue(f);
//			String sv = value.toString();
//
//			if (r.length() > 0)
//				r.append(separator);
//
//			r.append(sv);
//		}
//		String s = r.toString();
//		r.setLength(0);
//		r.trimToSize();
//		return s;
//	}

	public String sqlAssignmentList(Context context, Field[] fields, String[] fieldsToFilter) {
		return sqlAssignmentList(context, fields, " = ", " and ", fieldsToFilter);
	}

	public String getPrimaryKeyConditionAsAssignment(String sql, Performer performer, Context context) {
		String[] pkFieldNames = getPrimaryKeyFields(performer, context);
		Field[] pkFields = context.extractFieldsByNames(pkFieldNames);

		String r = sqlAssignmentList(context, pkFields, null);
		return r;
	}

	static void replaceFirst(Block sb, String from, String to) {
		// TODO avaliar performance
		Util.replaceFirst(sb.getOriginalBuilder(), from, to);
	}

	public String concretizeAll(String text) {
		return concretizeAll(text, null);
	}

	public String getRefSymbol() {
		return refSymbol;
	}

	public void setRefSymbol(String refSymbol) {
		this.refSymbol = refSymbol;
	}

//	public boolean isConcretizePostFix() {
//		return concretizePostFix;
//	}
//
//	public void setConcretizePostFix(boolean concretizePostFix) {
//		this.concretizePostFix = concretizePostFix;
//	}

	public boolean isCascadeReference() {
		return cascadeReference;
	}

	public void setCascadeReference(boolean cascadeReference) {
		this.cascadeReference = cascadeReference;
	}

	public DBS getProgram() {
		return program;
	}
}
