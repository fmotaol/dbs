package ext.path.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;

import ext.path.PathProcess;

public class JsonPathProcess extends PathProcess {

	public JsonPathProcess(String declarations) {
		super(declarations);
	}

	@Override
	public String docTargetKeyword() {
		return "**json";
	}

	@Override
	protected Object explore(String doc, String exp) {
		return JsonPath.read(doc, exp);
	}

	public Object convertOnlyJsonValue(Object value) {
		if (value == null)
			return null;

		if (isJsonStructureObject(value)) {
			String r = JsonPath.parse(value).jsonString();
			return r;
		}

		return value;
	}

	public boolean isJsonStructureObject(Object value) {
		if (value == null)
			return false;
		if (value instanceof LinkedHashMap)
			return true;
		return false;
	}

	@Override
	protected Object convertIfNecessary(Object v) {
		return convertOnlyJsonValue(v);
	}

	@Override
	protected void parseDataSetTarget(Object element, String exp) {

		if (element instanceof Map) {
			Map<String, Object> rec = (Map<String, Object>) element;
			detectColumns(rec);
			addRow(rec);
			return;
		}

		if (element instanceof List) {
			List<Object> ls = (List) element;
			for (Object row : ls) {
				// Aqui é recebido cada objeto como um Map de propriedades com valores

				if (!(row instanceof Map))
					throw new RuntimeException("Objeto não é um Map<String, Object>");

				Map<String, Object> rec = (Map<String, Object>) row;
				detectColumns(rec);
			}

			for (Object row : ls) {
				if (!(row instanceof Map))
					throw new RuntimeException("Objeto não é um Map<String, Object>");

				Map<String, Object> rec = (Map<String, Object>) row;
				addRow(rec);
			}

			return;
		}
	}

	@Override
	protected List<Object> extractValueList(Object values, String property) {
		if (values == null)
			throw new RuntimeException("Erro interno: objeto nulo");

		if (values instanceof List)
			return (List) values;

		if (values instanceof Collection)
			return new ArrayList<>((Collection) values);

		if (values instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) values;
			if (property == null)
				return new ArrayList<>(map.values());

			if (property.equalsIgnoreCase("keyset"))
				return new ArrayList<>(map.keySet());
			if (property.equalsIgnoreCase("values"))
				return new ArrayList<>(map.values());
			
		} else if (property != null)
			throw new RuntimeException("Propriedade " + property + " não suportada em " + values.getClass());

		if (values instanceof Map) {
			Map<String, Object> m = (Map<String, Object>) values;
			Collection<Object> s = m.values();
			return new ArrayList<>(s);
		}

		throw new RuntimeException("Objeto não foi reconhecido como uma lista: " + values.getClass());
	}

}
