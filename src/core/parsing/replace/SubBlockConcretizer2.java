package core.parsing.replace;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import core.args.UndefinedArgAction;
import core.dataset.DataSet;
import core.dataset.Field;
import core.dataset.Record;
import core.parsing.string.StringBuilder2;
import core.parsing.tree.CompositeString;
import core.parsing.tree.EnclosedBlock;
import core.parsing.tree.StringUnit;
import core.performer.Context;
import core.performer.Performer;
import util.Colls;
import util.Util;

@Deprecated //PROJETO INACABADO - TENTATIVA DE FAZER SUBBLOCKS SEREM RECURSIVOS
public class SubBlockConcretizer2 {

	private static String regexOpen = "??";
	private static String regexClose = "??";
	private static String regexOpenOrClose = "??";

	private StringConcretizer main;
	
	static Pattern patternOpen = Pattern.compile(regexOpen);
	static Pattern patternClose = Pattern.compile(regexClose);
	static Pattern patternOpenOrClose = Pattern.compile(regexOpenOrClose);

	public SubBlockConcretizer2(StringConcretizer main) {
		this.main = main;
	}
	
	private void concretizeMultiOpTranslation(StringBuilder2 sql) {
		if (!sql.contains("@$"))
			return;

		if (sql.contains("@$MULTIOP{", true))
			MultiOpTranslator.translate(sql);
	}

	private String extract(String[] array, int index) {
		if (index < 0 || index >= array.length)
			return null;
		return array[index];
	}
	
	public StringUnit concretizeSubBlock(StringUnit sql, Performer performer, Context context) {
		String[] parts = sql.split(patternOpenOrClose);
		if (parts.length == 0)
			throw new RuntimeException("Erro interno");
		
		if (parts[0].matches(regexOpen)) {
//			XXX
		}
//		CompositeString tree = parseTree(ss);
//		StringUnit r = tree.convertToBlock();
		//falta concretizar
//		return r;
		return null;
	}
	
	private record IndexedEnclosedBlock (EnclosedBlock block, int start, int end) {};
	
	private IndexedEnclosedBlock parseSubBlock(String[] parts, int from) {
		int start = from + 1;
		int bstart = from + 1; //bloco "before"
		int bend = parts.length - 1; //bloco "before"
		CompositeString b = new CompositeString();
		do {
			start = find(parts, start, patternOpen);
			if (start > 0) {
				IndexedEnclosedBlock ie = parseSubBlock(parts, start);
				
				bend = start - 1;
				for (int i = bstart; i <= bend; i++)
					b.add(null); //parts[i]);
			
				b.add(ie.block());
				bstart = ie.end;
			}
			
		} while (start < parts.length - 1 && start > 0);
		
		if (start > 0) {
			for (int i = from + 1; i <= bend; i++)
				b.add(null); //parts[i]);
		}
//		EnclosedBlock c = new EnclosedBlock(parts[0], b, parts[parts.length - 1]);
		//return new IndexedEnclosedBlock(c, );
		return null;
	}

	private int find(String[] parts, int from, Pattern pattern) {
		throw new RuntimeException("ainda não implementado");
	}

	public static final String regexSubQuery = "@query(=(?<conn>.*))?"
			+ "\\{(?<sql>[^\\}]*)\\}(\\[(?<qfield>.*)\\])?(::(native|\\.))?" + "|@previousquery(\\[(?<pqfield>.*)\\])?";

	private static final Pattern patternSubQuery = Pattern.compile(regexSubQuery,
			Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	private void concretizeSubQuery(StringBuilder2 sql, Performer performer, Context context) {
//		if (!concretizeSubqueries)
//			return;
		if (performer == null)
			return;
		if (!sql.contains("@query") && !sql.contains("@previousquery"))
			return;

		String field = null;

		Matcher matcher = patternSubQuery.matcher(sql.toString());

		boolean hasPreviousQuery = false;

		// Record record = null;
		DataSet ds = null;

		while (matcher.find()) {

			String pattern = matcher.group(0);
			String lowerPattern = pattern.toLowerCase();

			if (lowerPattern.startsWith("@query=") || lowerPattern.startsWith("@query{")) {

				String subSQL = matcher.group("sql");
				String connId = Util.ifNull(matcher.group("conn"), performer.getConcreteConnectionId(context));

				ds = performer.subquery(connId, subSQL, performer);
				performer.setPreviousSubqueryResult(ds);

//				previousQueryResult = queryResult;
				hasPreviousQuery = true;

				String f = matcher.group("qfield");
				if (f != null)
					field = f;

			} else if (pattern.startsWith("@previousquery")) {

				if (!hasPreviousQuery)
					throw new RuntimeException("Não existe query referida para @previousquery");

				String f = matcher.group("pqfield");
				if (f != null)
					field = f;

				ds = performer.getPreviousSubqueryResult();
				ds.beforeFirst();
//				if (psr != null)
//					field = psr.getField(fieldId);
			}

			boolean isNative = lowerPattern.endsWith("::native") || lowerPattern.endsWith("::.");

			String rowSep = performer.getSubqueryRowSeparator();
			String colSep = performer.getSubqueryColumnSeparator();
			concretizeSubqueryResult(sql, pattern, ds, field, isNative, rowSep, colSep);

		}

	}

	private void concretizeSubqueryResult(StringBuilder2 sql, String pattern, DataSet result, String field,
			boolean isNative, String rowSeparator, String colSeparator) {
		// field = null >> todos os campos
		int cols = 1;
		if (field == null)
			cols = result.getFieldCount();

		boolean tupleRequiresParenthesis = (cols > 1) && (rowSeparator.trim().equals(colSeparator.trim()));

		int rows = 0;

		String r = "";

		while (result.next()) {
			rows++;

			if (tupleRequiresParenthesis) {
				if (rows == 2)
					r = "(" + r + ")";
			}

			Record record = result.currentRecord();
			String s = getSubqueryFieldValues(record, field, isNative, colSeparator);
			if (tupleRequiresParenthesis) {
				if (rows >= 2)
					s = "(" + s + ")";
			}

			if (!"".equals(r))
				r += rowSeparator;

			r += s;
		}

		sql.replace(pattern, r, false, true);
	}

	private String getSubqueryFieldValues(Record record, String field, boolean isNative, String colSeparator) {
		if (field != null) {
			Field f = record.getField(field);
			String r = main.getFieldValue(record, f, isNative, !isNative);
			return r;
		} else {
			String r = "";
			for (Field f : record.getFields()) {
				String s = main.getFieldValue(record, f, isNative, !isNative);
				if (!"".equals(r))
					r += colSeparator;
				r += s;
			}
			return r;
		}
	}

	private static String getSubqueryFieldValues(DataSet result, String field, String colSeparator) {
		// TODO Auto-generated method stub

		throw new RuntimeException("ainda não implementado");
	}

	public static final String regexCondBlock = 
		    "@(?<type>if\\([^)]*\\)|ifhas|ifhasany)\\{(?<ifblock>[^}]*)\\}(?:@else\\{(?<elseblock>[^}]*)\\})?";	

	private static final Pattern patternCondBlock = Pattern.compile(regexCondBlock,
			Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	private void concretizeConditionalBlocks(StringBuilder2 sql, Context context) {

		sql.replace(patternCondBlock, (matcher) -> {
			String type = matcher.group("type");
			String ifblock = matcher.group("ifblock");
			if (ifblock == null)
				ifblock = "";
			String elseblock = matcher.group("elseblock");
			if (elseblock == null)
				elseblock = "";
			return activatedConditionalBlock(type, ifblock, elseblock, context);
		});

	}

	private List<String> listRefsByNullCondition(Map<String, String> valByRef, boolean ifNull, boolean ifNotNull) {
		return Colls.filter(valByRef.keySet(), (k) -> {
			String v = valByRef.get(k);

			boolean isNull = (v == null || "null".equals(v));
			if (isNull) {
				return ifNull;
			} else {
				return ifNotNull;
			}
		});
	}

	private String activatedConditionalBlock(String type, String ifblock, String elseblock, Context context) {
		StringBuilder2 ifb = new StringBuilder2(ifblock);
		main.concretizeReferences(ifb, context, UndefinedArgAction.NULL); //precisa ser NULL, pra identificar argumentos indefinidos

		if (type.equalsIgnoreCase("ifhas") || type.equalsIgnoreCase("ifhasany")) {
			List<String> nulls = listRefsByNullCondition(ifb.history().getReplacements(), true, false);
			if (nulls.isEmpty())
				return ifblock; // tanto ifhas quanto ifhasany
			
			// passou daqui, tem refs nulas

			if (type.equalsIgnoreCase("ifhas"))
				return elseblock;
			// passou daqui, é ifhasany

			List<String> notNulls = listRefsByNullCondition(ifb.history().getReplacements(), false, true);
			if (notNulls.isEmpty())
				return elseblock;
			else
				return ifblock;
		}

		if (type.startsWith("if(")) {
			String cond = extractIfCondition(type);
			Boolean b = main.concretizeAsBoolean(cond, context);
			if (b != null && b)
				return ifblock;
			else
				return elseblock;
		}

		throw new RuntimeException("Tipo de bloco condicional não suportado: " + type);
	}

	private String extractIfCondition(String type) {
		if (type == null || !type.startsWith("if(")) {
			return "";
		}
		int start = type.indexOf('(');
		int end = type.lastIndexOf(')');
		if (start == -1 || end == -1 || start >= end) {
			return "";
		}
		return type.substring(start + 1, end);
	}

	public void concretizeAll(StringBuilder2 sql, Performer performer, Context context) {
		concretizeMultiOpTranslation(sql);
		concretizeConditionalBlocks(sql, context);
		concretizeSubQuery(sql, performer, context);

	}

}
