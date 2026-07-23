package core.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;

import core.dataset.AbstractDataSet;
import core.dataset.Header;
import core.sql.Language;
import util.Strings;

public class FileDataSet extends AbstractDataSet {

	private String columnSeparator = ";";

	private String rowSeparator = "\r\n";

	private boolean firstRowIsHeader = true;

	private boolean includeFieldTypes = false;

	private BufferedReader reader;

	private boolean firstRead = true;

	private String[] fieldNames;

	private String[] fieldTypes;

	private int rowId = 0;

	private Integer limitRows;

	private Object[] values = null;

	public FileDataSet(FileConnection connection, String filePath, BufferedReader newReader) {
		super();
		this.connection = connection;
		this.filePath = filePath;
		this.reader = newReader;
	}

	public void loadHeadersIfNecessary() {
		if (firstRowIsHeader)
			parseHeader();
		if (includeFieldTypes)
			parseTypes();
	}

	private void parseHeader() {
		if (!next())
			throw new RuntimeException("Impossível extrair o cabeçalho de um arquivo vazio");
		if (!containsFieldNames(currentRow))
			throw new RuntimeException("Condição não tratada");
		headerRow = currentRow;
		fieldNames = headerRow.split(columnSeparator);
		fieldNames = Strings.trim(fieldNames);
		fieldNames = Strings.unquote(fieldNames);
		fieldNames = Strings.trim(fieldNames);
	}

	private boolean containsFieldNames(String row) {
		return true;
		// TODO Avaliar pela sintaxe dos nomes se são identificadores de colunas
	}

	private void parseTypes() {
		if (!next())
			throw new RuntimeException("Impossível extrair as informações de tipos de um arquivo vazio");

		fieldTypes = currentRow.split(columnSeparator);
	}

	private String currentRow;

	private FileConnection connection;

	private String filePath;

	public String getFilePath() {
		return filePath;
	}

	private String headerRow;

	public boolean ignoreEmptyRows = true;

	@Override
	public boolean internalNext() {
		if (limitRows != null)
			if (rowId > limitRows)
				return false;

		try {

			do {

				currentRow = readLine();
				if (currentRow == null)
					return false;

				if (!ignoreEmptyRows)
					break;

			} while (currentRow.equals(""));

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		values = null;
		rowId++;

		return tryToUpdatedRecordBuffers();

	}

	public static String removeBOM(String s) {
		if (s != null && s.startsWith("\uFEFF")) {
			return s.substring(1);
		}
		return s;
	}

	private String readLine() throws IOException {
		String r = reader.readLine();
		if (firstRead) {
			r = removeBOM(r);
		}
		firstRead = false;
		return r;
	}

	@Override
	public boolean hasNext() {
		if (limitRows != null)
			if (rowId > limitRows)
				return false;

		try {
			return reader.ready();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private boolean tryToUpdatedRecordBuffers() {
		// o retorno significa o valor a ser retornado pelo next (true/false)
		try {
			if (!currentRowIsHeader()) {

				try {

					updateRecordBuffers(false);

				} catch (NumberFormatException e) {

					if (currentRow.equals(headerRow)) {
						System.out.println("Cabeçalho repetido - ignorado pelo cursor");
						return next();
					} else
						throw e;

				}
			}
			return currentRow != null;

		} catch (NumberFormatException e) {
			System.out.println("Erro na linha " + rowId + ":");
			System.out.println(currentRow);
			throw e;
		}
	}

	private boolean currentRowIsHeader() {
		return firstRowIsHeader && (rowId == 1);
	}

	@Override
	public int getFieldCount() {
		return fieldNames.length;
	}

	@Override
	public String getFieldName(int index) {
		return fieldNames[index - 1];
	}

	@Override
	public void close() {
		try {
			connection.closeFile(filePath);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getRowId() {
		return rowId;
	}

//	@Override
//	protected Record createCurrentRecord() {
//		if (currentRow == null)
//			return new InvalidRecord(this);
//
//		// BufferedRecord r = createBufferedRecord();
//
//		DataSetRecord r = new DataSetRecord(this, rowId);
//
//		return r;
//	}

	@Override
	public boolean isActive() {
		if (reader == null)
			return false;
		return true;
	}

	@Override
	public String[] getFieldNames() {
		return fieldNames;
	}

	public void setFieldNames(String[] fieldNames) {
		this.fieldNames = fieldNames;
	}

	public String getColumnSeparator() {
		return columnSeparator;
	}

	public void setColumnSeparator(String columnSeparator) {
		this.columnSeparator = columnSeparator;
	}

	public boolean firstRowIsHeader() {
		return firstRowIsHeader;
	}

	public void setFirstRowIsHeader(boolean firstRowIsHeader) {
		this.firstRowIsHeader = firstRowIsHeader;
	}

	public void assignAdditionalParams(String param) {
		if (param.startsWith("types=")) {
			String[] ps = param.split("=", 2);
			if (ps.length != 2)
				throw new RuntimeException("Sintaxe inválida");

			String[] ss = ps[1].split(",");
			for (int i = 0; i < ps.length; i++) {
				ss[i] = ss[i].trim();
			}

			if (ps[1].contains(":"))
				fieldTypes = parseFieldTypesByName(ss);
			else
				fieldTypes = ss;
			return;
		}

		if (param.startsWith("limit=")) {
			String[] ps = param.split("=", 2);
			if (ps.length != 2)
				throw new RuntimeException("Sintaxe inválida");

			String slr = ps[1].trim();
			int limitRows = Integer.parseInt(slr);
			setLimitRows(limitRows);
			return;
		}

		throw new RuntimeException("Parâmetros não suportados: " + param);
	}

	private String[] parseFieldTypesByName(String[] decl) {
		String[] r = newUnknownArray();
		for (int i = 0; i < decl.length; i++) {
			String s = decl[i].trim();
			String[] ss = s.split(":", 2);
			if (ss.length != 2)
				throw new RuntimeException("Erro na declaração de tipos por nome");
			String name = ss[0].trim();
			int index = getIndexOfFieldName(name) - 1;
			r[index] = ss[1].trim();
		}
		return r;
	}

	private String[] newUnknownArray() {
		String[] r = new String[fieldNames.length];
		for (int i = 0; i < fieldNames.length; i++) {
			r[i] = "unknown";
		}
		return r;
	}

//	@Override @Deprecated
//	public boolean isFirstRow() {
//		throw new RuntimeException("ainda não implementado");
//		// TODO Auto-generated method stub
//	}

	public String getRowSeparator() {
		return rowSeparator;
	}

	public void setRowSeparator(String lineBreak) {
		this.rowSeparator = lineBreak;
	}

	public String getCurrentRow() {
		return currentRow;
	}

	public void setCurrentRow(String currentRow) {
		this.currentRow = currentRow;
	}

	public Integer getLimitRows() {
		return limitRows;
	}

	public void setLimitRows(Integer limitRows) {
		this.limitRows = limitRows;
	}

	public boolean secondRowContainTypes() {
		return includeFieldTypes;
	}

	public void setIncludeFieldTypes(boolean secondRowContainTypes) {
		this.includeFieldTypes = secondRowContainTypes;
	}

	@Override
	public Object readValue(int fieldIndex) {
		if (values == null) {
			try {
				parseValues();
			} catch (ParseException e) {
			}
		}

		return values[fieldIndex];
	}

	private void parseValues() throws ParseException {
		String[] sv = currentRow.split(columnSeparator, -1);
		if (sv.length != fieldNames.length)
			throw new RuntimeException("Quantidade de valores difere da quantidade de campos (" + sv.length + " contra "
					+ fieldNames.length + ")");
		String type = UNKNOWN_TYPE;

		for (int i = 0; i < sv.length; i++) {
//			String name = fieldNames[i];
			if (fieldTypes != null)
				type = fieldTypes[i];

			String s = sv[i];
			Object value;
			if (!"".equals(s))
				value = getLanguage().convertValue(s, type);
			else
				value = null;
			if (values == null)
				values = new Object[sv.length];
			values[i] = value;
		}
	}

	private Language getLanguage() {
		return Language.defaultLanguage();
	}

	@Override
	protected void putFields(Header header) {
		for (int i = 0; i < fieldNames.length; i++) {
			String name = fieldNames[i];
			String type;
			if (fieldTypes != null)
				type = fieldTypes[i];
			else
				type = null;
			header.addField(name, type);
		}
	}

	private static final String UNKNOWN_TYPE = "unknown";

		@Override
	public boolean isBeforeFirst() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

}
