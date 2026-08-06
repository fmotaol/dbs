package core.parsing.replace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import core.parsing.string.Block;
import core.parsing.string.ScrambleBlock;
import util.Util;

public class MultiOpTranslator {

	// private static final String contentRegex = "(?<content>.*)";

	private static final String regexContent = "(?<content>[^\\}]*)";

	public static final String regexMultiOp = Pattern.quote("@$MULTIOP{") + regexContent + "\\}";

	private static Pattern mainPattern = Pattern.compile(regexMultiOp, Pattern.DOTALL
			| Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	public static void translate(final Block sql) {

		Matcher matcher = mainPattern.matcher(sql.toString());

		while (matcher.find()) {

			String group = matcher.group(0);

			if (group.toUpperCase().startsWith("@$MULTIOP{")) {

				String content = matcher.group("content");
				String exp = generateExpression(content);
				// Util.replaceFirst(sql, Pattern.quote(group), exp);
				StringConcretizer.replaceFirst(sql, group, exp);
			}
		}

	}

	private static final String regexOpOptions = Pattern.quote("<>") + "|" + Pattern.quote("<=")
			+ "|" + Pattern.quote(">=") + "|" + Pattern.quote("=") + "|" + Pattern.quote("<") + "|"
			+ Pattern.quote(">") + "|";

	private static final String regexOp = "(?<op>" + regexOpOptions + ")";

	private static final String regexBloco1 = "(?<bloco1>.*)";

	private static final String regexBloco2 = "(?<bloco2>.*)";

	public static final String regexMultiOpRelation = "\\(" + regexBloco1 + "\\)\\s" + regexOp + "\\s\\("
			+ regexBloco2 + "\\)";

	private static Pattern relationPattern = Pattern.compile(regexMultiOpRelation, Pattern.DOTALL
			| Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	private static String generateExpression(String content) {
		content = content.trim();
		Matcher matcher = relationPattern.matcher(content);

		String exp = null;
		while (matcher.find()) {
			if (!content.equals(matcher.group(0)))
				throw new RuntimeException("Erro de sintaxe no MULTIOP");
			if (exp != null)
				throw new RuntimeException("Erro de definição de MULTIOP");
			String bloco1 = matcher.group("bloco1");
			String op = matcher.group("op");
			String bloco2 = matcher.group("bloco2");
			
			exp = generateExpression(bloco1, op, bloco2);
		}
		
		return exp;

	}

	private static String generateExpression(String bloco1, String op, String bloco2) {
		String[] fs1 = parseElementList(bloco1, ",", true);
		String[] fs2 = parseElementList(bloco2, ",", true);

		if (fs1.length == 0)
			throw new RuntimeException("Bloco de elementos vazio: " + bloco1);
		if (fs2.length == 0)
			throw new RuntimeException("Bloco de elementos vazio: " + bloco2);

		if (op.equals("=")) {
			String[] rs = generatePairExpressions(fs1, fs2, " = ");
			return Util.concat(rs, " and ");
		}

		if (op.equals("<>")) {
			String[] rs = generatePairExpressions(fs1, fs2, " <> ");
			return Util.concat(rs, " or ");
		}

		if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")) {
			// (matricula > @matricula) or
			// (matricula = @matricula and ano > @ano) or
			// (matricula = @matricula and ano = @ano and mes >= @mes)
			
			String opg = op.replace("=", "");

			Block exp = newStringBuilder();
			for (int i = 0; i < fs1.length; i++) {
				Block sub = newStringBuilder();
				for (int j = 0; j <= i; j++) {
					String s = null;
					if (i != j)
						s = Util.concat(fs1[j], " = ", fs2[j]);
					else {
						String o = opg;
						if (i == fs1.length - 1)
							o = op;
						s = Util.concat(fs1[j], " ", o, " ", fs2[j]);
					}
					sub.appendIfNotEmpty(" and ", s);
				}
				if (sub.length() > 0) {
					sub.insert(0, "(");
					sub.append(")");
				}
				sub.appendIfNotEmpty(" or ", sub.toString());
				sub.setLength(0);
				sub.trimToSize();
				// exp = Util.concatOnlyValues(exp, " or ", sub);
			}
			String s = exp.toString();
			exp.setLength(0);
			exp.trimToSize();
			return s;
		}

		throw new RuntimeException("Operador não suportado: " + op);

	}

	private static Block newStringBuilder() {
		return new ScrambleBlock();
	}

	private static String[] generatePairExpressions(String[] fs1, String[] fs2, String separator) {
		if (fs1.length != fs2.length)
			throw new RuntimeException("Blocos contém quantidades diferentes de elementos");

		String[] rs = new String[fs1.length];
		for (int i = 0; i < fs1.length; i++) {
			String exp = Util.concat(fs1[i], separator, fs2[i]);
			rs[i] = exp;
		}
		return rs;
	}

	private static String[] parseElementList(String s, String separator, boolean trim) {
		String[] ss = s.split(separator);

		if (trim)
			for (int i = 0; i < ss.length; i++)
				ss[i] = ss[i].trim();

		return ss;
	}
}
