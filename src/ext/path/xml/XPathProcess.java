package ext.path.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ext.path.PathProcess;

public class XPathProcess extends PathProcess {

	private XPath xpath;
	private Transformer transformer;

	public XPathProcess(String declarations) {
		super(declarations);
		XPathFactory f = XPathFactory.newInstance();
		xpath = f.newXPath();
	}

	@Override
	public String docTargetKeyword() {
		return "**xml";
	}

	@Override
	protected Object explore(String doc, String exp) {
		try {
			InputSource source = new InputSource(new StringReader(doc));
			NodeList nl = (NodeList) xpath.evaluate(exp, source, XPathConstants.NODESET);
			return nl;
		} catch (XPathExpressionException e) {
			String x = doc;
			if (x.length() > 100)
				x = x.substring(0, 100) + "...";
			throw new RuntimeException("Não foi possível processar xml: " + x);
		}
	}

	protected List<Object> convertList(NodeList nl, String property) {
		if (nl == null)
			return null;
		ArrayList<Object> r = new ArrayList<>();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			Object e = convertElement(n, property);
			r.add(e);
		}
		return r;
	}

	private Object convertElement(Node n, String property) {
		if (property != null && property.equalsIgnoreCase("textcontent"))
			return n.getTextContent();
		
		final short type = n.getNodeType();
		if (type == Node.ATTRIBUTE_NODE) {
			if (property == null)
				return n.getNodeValue();
			if (property.equalsIgnoreCase("name"))
				return n.getNodeName();
			if (property.equalsIgnoreCase("value"))
				return n.getNodeValue();
		}
		
		if (property != null)
			throw new RuntimeException("Propriedade não suportada: " + property);
		
		if (type == Node.ELEMENT_NODE) {
			String r = nodeToXMLString(n);
			return r;
		}
		
		if (type == Node.TEXT_NODE) 
			return n.getTextContent();		

		if (type == Node.COMMENT_NODE) 
			return n.getTextContent();
		
		if (type == Node.CDATA_SECTION_NODE) 
			return n.getTextContent();		

		throw new RuntimeException("Tipo de Nodo não suportado: " + type);
	}

	private String nodeToXMLString(Node node) {
		Transformer t = getTransformer();
		return convertToXMLString(node, t);
	}

	private String convertToXMLString(Node node, Transformer t) {
		StringWriter w = new StringWriter();
		try {
			t.transform(new DOMSource(node), new StreamResult(w));
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
		return w.toString();
	}

	private Transformer getTransformer() {
		if (transformer != null)
			return transformer;
		
		TransformerFactory tf = TransformerFactory.newInstance();
		
		try {
			transformer = tf.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		}
		
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		return transformer;
	}

	@Override
	protected Object convertIfNecessary(Object v) {
		return v;
	}

	@Override
	protected void parseDataSetTarget(Object result, String exp) {
		if (result == null)
			throw new RuntimeException("Lista nula");
		
		NodeList nl;
		try {
			nl = (NodeList) result;
		} catch (ClassCastException e) {
			throw new RuntimeException("Resultado não é um NodeList");
		}
		
		ArrayList<Object> r = new ArrayList<>();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			Map<String, Object> row = extractAttributes(n);
			detectColumns(row);
		}

		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			Map<String, Object> row = extractAttributes(n);
			addRow(row);
		}

	}

	private Map<String, Object> extractAttributes(Node n) {
		HashMap<String, Object> r = new HashMap<>();
		NamedNodeMap as = n.getAttributes();
		for (int i = 0; i < as.getLength(); i++) {
			Node a = as.item(i);
			String name = a.getNodeName();
			Object value = a.getNodeValue();
			r.put(name, value);
		}
		return r;
	}

	@Override
	protected List<Object> extractValueList(Object values, String property) {
		if (values == null)
			throw new RuntimeException("Lista nula");
		if (values instanceof NodeList)
			return convertList((NodeList) values, property);
		throw new RuntimeException("Classe não prevista: " + values.getClass());
	}

	
}
