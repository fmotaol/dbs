package ext.path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import core.dataset.ColumnarDataSet;

public abstract class PathProcess {
	
	private String declarations;
	
	private String doc;
	
	private TreeSet<String> declaredTargets = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

	private ColumnarDataSet dataSet;

	public PathProcess(String declarations) {
		this.declarations = declarations;
	}
	
	public void parse() {
		dataSet = new PathDataSet(this);
		parseDeclarations(declarations);
	}
	
	public abstract String docTargetKeyword();
	
	private void parseDeclarations(String input) {
		List<Declaration> declarations = new ArrayList<>();
		String[] lines = input.split("\\n");
		StringBuilder docBuffer = new StringBuilder();
		boolean collectingDoc = false;
		String currentTarget = null;

		// Primeira passada: coletar todas as declara��es
		String docTargetKeyword = docTargetKeyword();
		for (String line : lines) {
			String trimmedLine = line.trim();
			if (trimmedLine.isEmpty())
				continue;

			if (collectingDoc) {
				if (trimmedLine.matches("^\\S+\\s*=\\s*\\S+.*")) {
					declarations.add(new Declaration(currentTarget, docBuffer.toString().trim(), null));
					collectingDoc = false;
					docBuffer.setLength(0);
				} else {
					docBuffer.append(line).append("\n");
					continue;
				}
			}

			if (trimmedLine.startsWith("//"))
				continue;
			
			if (trimmedLine.startsWith(docTargetKeyword)) {
				String[] parts = trimmedLine.split("\\s*=\\s*", 2);
				if (parts.length == 2) {
					currentTarget = parts[0];
					collectingDoc = true;
					docBuffer.setLength(0);
					docBuffer.append(parts[1]).append("\n");
				}
				continue;
			}

			String regex = "^(\\S+)\\s*=\\s*(\\S+)(?:\\s*:\\s*(\\S+))?$";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(trimmedLine);

			if (matcher.find()) {
				String target = matcher.group(1);
				String exp = matcher.group(2);
				String property = matcher.group(3);
				declarations.add(new Declaration(target, exp, property));
			}
		}

		if (collectingDoc && currentTarget != null) {
			declarations.add(new Declaration(currentTarget, docBuffer.toString().trim(), null));
		}

		// Segunda passada: processar na ordem correta (**doc primeiro)
		for (Declaration decl : declarations) {
			if (decl.target.startsWith(docTargetKeyword)) {
				parse(decl);
				break; // Só processa o primeiro **json/**xml
			}
		}

		// Processar as demais declara��es
		for (Declaration decl : declarations) {
			if (!decl.target.startsWith(docTargetKeyword)) {
				parse(decl);
			}
		}
	}


	void parse(Declaration decl) {
		
		if (declaredTargets.contains(decl.target))
			throw new RuntimeException("Declara��o duplicada: " + decl.target);
		declaredTargets.add(decl.target);

		final String docTargetKeyword = docTargetKeyword(); //**doc = **json ou **xml
		
		if (decl.target.equalsIgnoreCase(docTargetKeyword)) {
			if (decl.property != null)
				throw new RuntimeException("N�o � permitida declara��o de propriedade em " + docTargetKeyword);

			this.doc = decl.exp;
			return;
		}
		
		if (doc == null)
			throw new RuntimeException(docTargetKeyword + " n�o declarado");
		
		Object r = explore(doc, decl.exp);
		if (decl.target.equalsIgnoreCase("**dataset")) {
			if (decl.property != null)
				throw new RuntimeException("N�o � permitida declara��o de propriedade em **dataset");

			parseDataSetTarget(r, decl.exp);
			return;
		}
		// a partir daqui � parse de field

		if (r == null)
			throw new RuntimeException("Condi��o n�o tratada PathProcessor: objeto nulo");

//		if (decl.property != null) {
//			r = processProperty(r, decl.property);
//		}

		List<Object> l = extractValueList(r, decl.property);
		dataSet.addColumn(decl.target, l);
	}

	protected abstract Object explore(String doc, String exp);

	protected abstract void parseDataSetTarget(Object element, String exp);

	protected void createEmptyColumns(Set<String> fields) {
		dataSet.createEmptyColumns(fields);
	}

	static class Declaration {
		String target;
		String exp;
		String property;

		Declaration(String target, String exp, String property) {
			this.target = target;
			this.exp = exp;
			this.property = property;
		}

		@Override
		public String toString() {
			return "Declaration [target=" + target + ", exp=" + exp + ", property=" + property + "]";
		}
		
	}

	public String getDoc() {
		return doc;
	}

	public ColumnarDataSet getDataSet() {
		return dataSet;
	}

	protected abstract Object convertIfNecessary(Object v);


	protected void detectColumns(Map<String, Object> record) {
		Set<String> fields = record.keySet();
		createEmptyColumns(fields);
	}

	protected void addRow(Map<String, Object> record) {
		dataSet.addRow(record);
	}

	protected abstract List<Object> extractValueList(Object values, String property);

}
