package core.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import core.DBS;
import core.dataset.DataSet;
import core.dataset.ElementaryDataSet;
import core.dataset.Field;
import core.dataset.Record;
import core.parsing.CommandParser;
import core.parsing.Parse;
import core.performer.Batch;
import core.performer.Context;
import core.performer.DBSConnection;
import core.performer.Performer;
import core.performer.Result;
import core.performer.TargetPerformer;
import core.performer.VirtualBatch;
import core.sql.Language;
import util.Strings;
import util.Util;

public class FileConnection extends DBSConnection {

	public Charset charset = StandardCharsets.UTF_8;

	private HashMap<String, FileRef> fileRefByFileName = new HashMap<String, FileRef>();

	private FileRef selectedFileRef;

	private boolean includeFieldTypes = false;

	@Override
	public DataSet query(String fileQuery, Performer invoker) {
		fileQuery = fileQuery.trim();
		String cmd = extractCommand(fileQuery);
		if (cmd.equalsIgnoreCase("list"))
			return listFilesQueryByCommand(fileQuery);

		if (cmd.equalsIgnoreCase("load"))
			return loadFileByCommand(fileQuery, invoker);

		if (cmd.equalsIgnoreCase("read"))
			return readFileByCommand(fileQuery, invoker);

		if (cmd.equalsIgnoreCase("properties"))
			return getFilePropertiesByCommand(fileQuery, invoker);

		throw new RuntimeException("Comando não suportado para Query: " + fileQuery);
	}

	@Override
	public Result execute(String fullCommand, Performer invoker, Context context) {
		try {
			return internalExecute(fullCommand, invoker);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Result internalExecute(String fullCommand, Performer invoker) throws IOException {
		fullCommand = fullCommand.trim();
		fullCommand = CommandParser.clearCommentedLines(fullCommand);
		String cmd = extractCommand(fullCommand);
		if (cmd.equalsIgnoreCase("rename")) {
			renameFileByCommand(fullCommand);
			return new Result(0);
		}

		if (cmd.equalsIgnoreCase("move")) {
			moveFileByCommand(fullCommand);
			return new Result(0);
		}

		if (cmd.equalsIgnoreCase("close")) {
			closeFileByCommand(fullCommand);
			return new Result(0);
		}

		if (cmd.equalsIgnoreCase("delete")) {
			deleteFile(fullCommand);
			return new Result(0);
		}
		if (cmd.equalsIgnoreCase("create")) {
			createFileByCommand(fullCommand);
			return new Result(0);
		}

		if (cmd.equalsIgnoreCase("open")) {
			openFileByCommand(fullCommand);
			return new Result(0);
		}

		if (cmd.equalsIgnoreCase("choose")) {
			chooseFileByCommand(fullCommand);
			return new Result(0);
		}

		if (cmd.equalsIgnoreCase("write")) {
			writeFileByCommand(fullCommand, invoker.getRowSeparator());
			return new Result(0);
		}

		throw new RuntimeException("Comando não suportado: " + fullCommand);
	}

	private void writeFileByCommand(String fullCommand, String rowSeparator) throws IOException {
		String[] params = extractParams("write".length(), fullCommand, 1);
		assertParamsCount(params, 1);
		writeln(params[0], rowSeparator);
	}

	private void assertParamsCount(String[] params, int i) {
		assertParamsCount(params, i, i);
	}

	private void writeln(String row, String rowSeparator) throws IOException {
		FileRef ref = selected();
		ref.write(row);
		ref.write(rowSeparator);
	}

	private FileRef selected() {
		if (selectedFileRef == null)
			throw new RuntimeException("Não existe arquivo selecionado");
		return selectedFileRef;
	}

	private FileRef getFileRef(String filePath) {
		FileRef r = findFileRef(filePath);
		if (r == null)
			throw new RuntimeException("Arquivo não está aberto: " + filePath);
		return r;
	}

	private void chooseFileByCommand(String fullCommand) {
		String[] params = extractParams("choose".length(), fullCommand, 1);
		assertParamsCount(params, 1);
		String filePath = params[0];
		selectedFileRef = getFileRef(filePath);
	}

	private void openFileByCommand(String fullCommand) throws IOException {
		String[] params = extractParams("choose".length(), fullCommand, 1);
		assertParamsCount(params, 1);
		String filePath = params[0];
		openFileForWrite(filePath, false, true);
	}

	private BufferedReader openFileForRead(String filePath) {
		FileRef ref = findFileRef(filePath);
		if (ref != null)
			throw new RuntimeException("Arquivo já está aberto: " + filePath);

		ref = new FileRef(filePath);
		ref.openReader();
		fileRefByFileName.put(filePath, ref);
		return ref.reader;
		// NÃO SELECIONA ESTE! OS COM READER SERVEM APENAS PARA OS DATASETS
	}

	private FileRef openFileForWriteDefaultImporting(String filePath, boolean canCreate, boolean append,
			TargetPerformer invoker, DataSet source) {

		FileRef r = openFileForWrite(filePath, canCreate, append);

		String[] fieldNames = source.getFieldNames();
		String columnSeparator = invoker.getColumnSeparator();
		String rowSeparator = invoker.getRowSeparator();
		if (r.fileIsEmpty() && invoker.firstRowIsHeader())
			try {
				writeHeader(fieldNames, columnSeparator, rowSeparator, invoker);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		return r;

	}

	private FileRef openFileForWrite(String filePath, boolean canCreate, boolean append) {
		FileRef ref = findFileRef(filePath);
		if (ref == null)
			ref = new FileRef(filePath);

		if (!ref.file.exists()) {
			if (!canCreate) {
				throw new RuntimeException("Arquivo não existe: " + filePath);
			}
		}

		try {
			ref.openWriter(append);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		fileRefByFileName.put(filePath, ref);

		selectedFileRef = ref;
		return ref;
	}

	private void createFileByCommand(String fullCommand) throws IOException {
		String[] params = extractParams("create".length(), fullCommand, 1);
		assertParamsCount(params, 1);
		String filePath = params[0];
		openFileForWrite(filePath, true, false);
	}

	private void closeFileByCommand(String fullCommand) throws IOException {
		String[] params = extractParams("close".length(), fullCommand, 1);
		assertParamsCount(params, 1);
		String filePath = params[0];
		closeFile(filePath);
	}

	public void closeFile(String filePath) throws IOException {
		if (isSelected(filePath))
			unselect();
		FileRef r = findFileRef(filePath);
		if (r != null)
			r.close();
		else
			warning("Arquivo não está aberto: ", filePath);
	}

	private FileRef findFileRef(String filePath) {
		return fileRefByFileName.get(filePath);
	}

	File file(String filePath) {
		filePath = filePath.trim();
		filePath = unquote(filePath);
		return new File(filePath);
	}

	private void warning(String msg, Object... objects) {
		System.out.print(msg);
		for (Object o : objects)
			System.out.print(o);
	}

	private DataSet listFilesQueryByCommand(String fullCommand) {
		String dir = "";
		String[] params = extractParams(4, fullCommand, 1);
		assertParamsCount(params, 0, 1);
		if (params.length == 0)
			dir = System.getProperty("user.dir");
		else
			dir = unquote(params[0]);
		FileListDataSet r = new FileListDataSet(dir);
		return r;
	}

	private DataSet loadFileByCommand(String fullCommand, Performer invoker) {
		String[] params = extractParams("open".length(), fullCommand, null);
		assertParamsCount(params, 1, null);
		String fileName = unquote(params[0]);
		FileDataSet r = new FileDataSet(this, fileName, openFileForRead(fileName));
		r.setColumnSeparator(invoker.getColumnSeparator());
		r.setRowSeparator(invoker.getRowSeparator());
		r.setFirstRowIsHeader(invoker.firstRowIsHeader());
		r.setIncludeFieldTypes(includeFieldTypes);

		r.loadHeadersIfNecessary();

		for (int i = 1; i < params.length; i++) {
			r.assignAdditionalParams(params[i]);
		}

		return r;
	}

	private DataSet readFileByCommand(String fullCommand, Performer invoker) {
		String[] params = extractParams("read".length(), fullCommand, null);
		assertParamsCount(params, 1, null);
		String fileName = unquote(params[0]);
		ElementaryDataSet r = new ElementaryDataSet("content", "text", readFileContent(fileName));

		for (int i = 1; i < params.length; i++) {
			System.out.println("AVISO: parâmetro " + params[i] + " desconsiderado");
		}

		return r;
	}

	public String readFileContent(String filePath) {
		checkFileExists(filePath);

		Path path = Paths.get(filePath);
		try {
//			return Files.readString(path);

			byte[] bytes = Files.readAllBytes(path);
			String r = new String(bytes, "UTF-8");
			return r;
		} catch (Throwable e) {
			throw new RuntimeException("Não foi possível ler o conteúdo do arquivo " + filePath);
		}
	}

	public static void checkFileExists(String filePath) {
		File f = new File(filePath);
		if (!f.exists())
			throw new RuntimeException(
					"Arquivo não existe: " + filePath + " (diretório atual: " + DBS.getCurrentDir() + ")");
	}

	private DataSet getFilePropertiesByCommand(String fullCommand, Performer invoker) {
		String[] params = extractParams("properties".length(), fullCommand, null);
		assertParamsCount(params, 1, null);
		String fileName = unquote(params[0]);
		FilePropsDataSet r = new FilePropsDataSet(fileName);

//		r.setColumnSeparator(invoker.getColumnSeparator());
//		r.setRowSeparator(invoker.getRowSeparator());
//		r.setFirstRowIsHeader(invoker.firstRowIsHeader());
//		r.setIncludeFieldTypes(includeFieldTypes);
//
//		r.loadHeadersIfNecessary();

//		for (int i = 1; i < params.length; i++) {
//			r.assignAdditionalParams(params[i]);
//		}

		return r;
	}

	private String extractCommand(String sentence) {
		int i = sentence.indexOf(' ');
		String r;
		if (i == -1)
			r = sentence;
		else
			r = sentence.substring(0, i);
		return r;
	}

	@Deprecated
	private String[] extractParams_Old(int prefixLength, String fullCommand, Integer maxParams) {
		String ps = fullCommand.trim();
		ps = ps.substring(prefixLength).trim();
		String[] r;
		String regex = "\\s+";
		if (maxParams != null)
			r = ps.split(regex, maxParams);
		else
			r = ps.split(regex);
		return r;
	}

	private String[] extractParams(int prefixLength, String fullCommand, Integer maxParams) {
		String ps = fullCommand.trim();
		ps = ps.substring(prefixLength).trim();
		String[] r = Parse.arguments(ps);
		return r;
	}

	private void assertParamsCount(String[] params, int min, Integer max) {
		if (params.length < min)
			throw new RuntimeException("Faltando parâmetros");
		if (max != null)
			if (params.length > max)
				throw new RuntimeException("Excesso de parâmetros");
	}

	@Override
	public void setAutoCommit(Boolean autoCommit) {
		// sem efeito
	}

	private void renameFileByCommand(String fullCommand) {
		String[] params = extractParams("rename".length(), fullCommand, 1);
		assertParamsCount(params, 2);
		String fileName = unquote(params[0]);
		String newName = unquote(params[1]);
		File f = new File(fileName);
		File n = new File(newName);
		f.renameTo(n);
	}

	private void moveFileByCommand(String fullCommand) throws IOException {
		String[] params = extractParams("move".length(), fullCommand, 2);
		assertParamsCount(params, 2);
		String fileName = unquote(params[0].trim());
		String destPath = unquote(params[1].trim());

		if (isOpen(fileName))
			closeFile(fileName);

		File f = new File(fileName);
		String name = f.getName();
		Path pathFile = FileSystems.getDefault().getPath(fileName);
		Path pathTo = FileSystems.getDefault().getPath(destPath + "\\" + name);
		Files.move(pathFile, pathTo);
	}

	private boolean isOpen(String fileName) {
		FileRef r = findFileRef(fileName);
		return r != null;
	}

	private boolean isSelected(String fileName) {
		FileRef r = findFileRef(fileName);
		if (r == null)
			return false;
		return r == selectedFileRef;
	}

	private static String unquote(String fileName) {
		return Strings.unquote(fileName, '\'');
//		return fileName.trim().replace("'", "");
	}

	private void deleteFile(String fileName) {
		fileName = unquote(fileName);
		File f = new File(fileName);
		f.delete();
	}

	@Override
	public Batch createBatch(Performer performer) {
		return new VirtualBatch(this, performer);
	}

	@Override
	public void defaultStartImportingData(TargetPerformer target) {
		// nada
	}

	private void writeHeader(String[] fieldNames, String columnSeparator, String rowSeparator, TargetPerformer invoker)
			throws IOException {

		FileRef r = selected();
		boolean first = true;
		for (String f : fieldNames) {
			if (!first) {
				r.write(columnSeparator);
			} else
				first = false;
			r.write(f);
		}
		r.write(rowSeparator);
		r.flush();

		if ((invoker != null) && (invoker.showStatus)) {
			String hp = " -> " + Util.concat(fieldNames, ";");
			invoker.println("[" + DBS.formatTime() + "] Importado cabeçalho " + hp);
		}

	}

	public void writeRow(FileRef fileRef, Record record, Performer invoker) throws IOException {
		boolean first = true;

		for (int i = 0; i <= record.getFieldCount() - 1; i++) {
			if (!first) {
				fileRef.write(invoker.getColumnSeparator());
			} else
				first = false;
			Object value = record.getValue(i);
			String v = invoker.formatValue(value);
			fileRef.write(v);
		}

		fileRef.write(invoker.getRowSeparator());
		fileRef.flush();
	}

	@Deprecated
	public void writeRow_Old(FileRef fileRef, Record record, Performer invoker) throws IOException {
		boolean first = true;

		for (Field f : record.getFields()) {
			if (!first) {
				fileRef.write(invoker.getColumnSeparator());
			} else
				first = false;
			Object value = record.getValue(f);
			String v = invoker.formatValue(value);
			fileRef.write(v);
		}

		fileRef.write(invoker.getRowSeparator());
		fileRef.flush();
	}

	public void unselect() {
		selectedFileRef = null;
	}

	@Override
	public void defaultImportRow(TargetPerformer target, Context context) {

		FileRef ref = fileRefByTemplateFileName(target, context);
		select(ref);
		try {
			writeRow(ref, context.record, target);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void select(FileRef ref) {
		selectedFileRef = ref;
	}

	private FileRef fileRefByTemplateFileName(TargetPerformer target, Context context) {
		String fileName = target.getConcreteOutputFileName(context);
		boolean append = fileName.equals(target.getAppendingFile());
		FileRef r = findFileRef(fileName);
		if (r == null)
			r = openFileForWriteDefaultImporting(fileName, true, append, target, context.dataSet);

		if (!append)
			target.setAppendingFile(fileName);

		return r;
	}

	@Override
	public void defaultEndImportingData(TargetPerformer target) {
		target.setAppendingFile(null);
		closeSelected();
	}

	private void closeSelected() {
		FileRef s = selectedFileRef;
		if (s == null)
			return;

		try {
			closeFile(s.path);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	class FileRef {
		String path;

		File file;

		BufferedReader reader;

		private FileWriter writer;

		public FileRef(String filePath) {
			this.path = filePath;
			this.file = new File(path);
		}

		@Deprecated
		public boolean fileIsEmpty() {
			return file.length() == 0;
		}

		public void flush() throws IOException {
			writer.flush();

		}

		public void write(String row) throws IOException {
			writer.write(row);
		}

		public void close() throws IOException {
			if (reader != null)
				reader.close();

			if (writer != null) {
				writer.close();
			}
		}

		public void openWriter(boolean append) throws IOException {
			writer = new FileWriter(file, append);
		}

		public void openReader() {
			try {
				reader = newReader();
//				treat_UTF8_BOM();

			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private BufferedReader newReader() throws FileNotFoundException {
//			reader = new BufferedReader(new FileReader(file));

			InputStreamReader in = new InputStreamReader(new FileInputStream(file), charset);
			return new BufferedReader(in);
		}

		private void treat_UTF8_BOM() throws IOException {
			// Pular o BOM se existir
			reader.mark(3);
			char[] bom = new char[3];
			int read = reader.read(bom);

			if (read == 3 && (bom[0] & 0xFF) == 0xEF && (bom[1] & 0xFF) == 0xBB && (bom[2] & 0xFF) == 0xBF) {
				// era BOM
			} else {
				// não era BOM, volta
				reader.reset();
				reader = newReader();
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		for (FileRef r : fileRefByFileName.values())
			r.close();
	}

	public boolean includeFieldTypes() {
		return includeFieldTypes;
	}

	public void setIncludeFieldTypes(boolean secondRowContainTypes) {
		this.includeFieldTypes = secondRowContainTypes;
	}

	@Override
	public void reconnect() {
		// nada
	}

	@Override
	public void close() {
		// TODO Checar eventuais arquivos abertos e fechar
	}

	private Language language = Language.defaultLanguage();

	@Override
	public Language getLanguage() {
		return language;
	}

	@Override
	public String getId() {
		return "files";
	}

}
