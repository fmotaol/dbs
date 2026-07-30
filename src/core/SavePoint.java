package core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Predicate;

import core.args.Argument;
import core.args.Argument.Origin;
import core.args.UndefinedArgAction;
import core.dataset.Record;
import util.FileUtil;
import util.Strings;
import util.Util;

public class SavePoint {

	public long getLastSave() {
		return lastSave;
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	private DBS program;

	private boolean withArgs;

	private File file;

	public SavePoint(DBS program, boolean withArgs) {
		this.program = program;
		this.withArgs = withArgs;
		this.file = null; //ser� carregado depois
	}

	public SavePoint(DBS program, File file) {
		this.program = program;
		this.file = file;
	}

	private SavePoint(DBS program) {
		this.program = program;
	}

	HashMap<String, String> valuesToSave = new HashMap<String, String>();

	ArrayList<String> items = new ArrayList<String>();

	ArrayList<String> doneBlocks = new ArrayList<String>();

	private long lastSave = (new Date()).getTime();

	private long lastUpdate = (new Date()).getTime();

	public boolean REGISTER_DONE_ELEMENTS = false;

	public static String generateFileName(DBS program, boolean withArgs) {
		if (withArgs)
			return FileUtil.generateFileNameForArguments(program, "#", ".sav", false, false);
		else
			return program.getDBSFileName() + ".sav";
	}

	public void register(String device, String[] columns, Record record) {
		if (columns == null)
			return;

		for (String col : columns) {
			String sv = record.getValueAsString(col);
			register(device, col, sv);
		}

//		manageAutoSave();
	}

	public void register(String device, String property, String value) {
		if (property == null)
			return;
		String fullPath;
		if ((device == null) || ("".equals(device)))
			fullPath = property;
		else
			fullPath = device + "." + property;

		items.remove(fullPath);
		items.add(fullPath);
		valuesToSave.put(fullPath, value);
		if (device != null) {
			items.remove(device);
			doneBlocks.remove(device);
		}

		lastUpdate = (new Date()).getTime();

//		manageAutoSave();
	}

	public void registerAsDone(String deviceName) {
		if (!REGISTER_DONE_ELEMENTS)
			return;

		removeEntries(deviceName);
		items.add(deviceName);
		doneBlocks.add(deviceName);

//		manageAutoSave(); 

	}

	public void removeEntries(String blockName) {
		String n = blockName + ".";
		HashMap<String, String> vs = (HashMap<String, String>) valuesToSave.clone();
		for (String c : vs.keySet()) {
			if (c.startsWith(n) && !isPermanentProperty(c)) {
				items.remove(c);
				valuesToSave.remove(c);
			}
		}
		items.remove(blockName);
		doneBlocks.remove(blockName);

	}

	private boolean isPermanentProperty(String p) {
		String[] ss = p.split("\\.");
		String s = ss[ss.length - 1];
		boolean r = s.startsWith("$");
		return r;
	}

//	public void load() throws IOException {
//		// tenta carregar tamb�m o filename.sav com o mesmo nome do arquivo sql
//		String name = program.getDBSFileName() + ".sav";
//		File f = new File(name);
//		if (f.exists())
//			load(DataScope.PROPERTIES);
//
////		if (!file.getName().equals(f.getName()))
////		if (file.exists())
//		load(DataScope.PROPERTIES);
//
//
//	}
//
	public void finish() {
		if (file != null)
			file.delete();
	}

	public synchronized void save() {

		updateGlobalProperties();

		try {

			// file.delete();
			writeFileContent();

		} catch (Exception e) {
			Util.throwAsRuntimeException(e);
		}

	}

	private void updateGlobalProperties() {
		register(null, "$dbsfile", program.getDBSFile());
		Long v = program.getPreviousSpentTime() + program.getCurrentSpentTime();
		register(null, "$previousSpentTime", v.toString());
		for (Argument a : program.arguments) {
			if (a.getOrigin() != Origin.DEFAULT)
				register(null, "$" + a.getFullId(), a.getValue());
			else
				register(null, "$" + a.getFullId(), "$default");
		}
	}

	private void writeFileContent() throws IOException, InterruptedException {
		boolean fileWriteError = false;
		boolean showedWarning = false;

//		assertFileName();
		
		do {

			try {

//				if (file.canWrite()) {

				internalWriteFileContent();
				fileWriteError = false;

//				} else
//					fileWriteError = true;

			} catch (IOException e) {
				System.out.println("Erro: " + e.getMessage());
				fileWriteError = true;
			}

			if (fileWriteError) {
				if (!showedWarning) {
					System.out.println("AVISO: Aguardando libera��o de escrita do arquivo " + file.getName());
					showedWarning = true;
				}
				Thread.sleep(400);
			}

		} while (fileWriteError);

	}

	private void internalWriteFileContent() throws IOException {
		FileWriter fw = new FileWriter(getFile(), false);
		BufferedWriter writer = new BufferedWriter(fw);
		try {
			for (String s : items) {

				if (REGISTER_DONE_ELEMENTS)
					if (doneBlocks.contains(s)) {
						writer.write(s + " is done");
						writer.newLine();
					}

				if (valuesToSave.containsKey(s)) {
					String v = valuesToSave.get(s);
					writer.write(s + "=" + v);
					writer.newLine();
				}
			}

//			writer.flush();
			lastSave = (new Date()).getTime();

		} finally {
			writer.close();
		}
	}

	public enum DataScope {
		DBSFILE, ARGS, PROPERTIES
	};

	public void load(DataScope scope) {
//		assertFileName();
		try {
			FileReader fr = new FileReader(getFile());
			BufferedReader r = new BufferedReader(fr);
			try {
				while (r.ready()) {
					String row = r.readLine();
					parseRowContent(row, scope);

				}
			} finally {
				r.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void parseRowContent(String row, DataScope scope) {
		row = row.trim();

		if ("".equals(row))
			return;

		if (row.endsWith(" is done") && !row.contains("=")) {
			if (REGISTER_DONE_ELEMENTS) {
				String[] ss = row.split(" is done");
				String name = ss[0];
				SavePointRestoreable d = (SavePointRestoreable) program.findDevice(name);
				if (d == null)
					throw new RuntimeException("Dispositivo não localizado: " + name);
				d.setAsDone();
			}
			return;
		}

		if (row.contains("=")) {
			parseProperty(row, scope);
			return;
		}

		throw new RuntimeException("Conteúdo do arquivo de SavePoint não reconhecido: " + row);
	}

	private void parseProperty(String row, DataScope scope) {
		String[] rs = row.split("=");
		String name = rs[0];
		String value = row.substring(name.length() + 1);

		if (scope == DataScope.DBSFILE) {
			if (name.equals("$dbsfile")) {
				parseLoadDBSFile(value);
			}
			return;
		}

		if (scope == DataScope.ARGS) {
			if (name.startsWith("$arg[")) {
				parseArgProperty(name, value);
			}
			return;
		}

		if (scope == DataScope.PROPERTIES) {

			String[] ns = name.split("\\.");
			String property = ns[ns.length - 1];
			SavePointRestoreable d = null;
			String device = null;

			if (ns.length > 1) { // j� aponta para o device
				int se = name.length() - property.length();
				if (se > 0) {
					se -= 1;
					device = name.substring(0, se);
					d = (SavePointRestoreable) program.findDevice(device);
				}
			} else {
				d = findDeviceBySavePointProperty(property);
				if (d != null)
					device = d.getFullName();
			}

			if (isPermanentProperty(property))
				register(device, property, value);

			if (d != null)
				d.restoreSavePointProperty(property, value);
			else
				program.restoreStaticSavePointProperty(property, value);
		}
	}

	private void parseLoadDBSFile(String value) {
		program.setOpenedFile(value);
	}

	private void parseArgProperty(String property, String value) {
		String name = property.trim();
		name = Strings.removeStart(name, "$arg[");
		name = Strings.removeEnd(name, "]");

		Argument a = program.getArgByName(name);
		if (a == null)
			throw new RuntimeException("Inconsistência de savepoint: argumento não encontrado: " + name);

		value = value.trim();
		if (value.equals("$default"))
			a.setDefaultValue();
		else
			a.setValue(value, Origin.SAV_FILE);
	}

	private SavePointRestoreable findDeviceBySavePointProperty(String property) {
		Predicate<Device> c = new Predicate<Device>() {

			@Override
			public boolean test(Device d) {
				if (!(d instanceof SavePointRestoreable))
					return false;
				SavePointRestoreable s = (SavePointRestoreable) d;
				String[] cols = s.getSavePointColumns();
				if (cols != null)
					if (Strings.contains(cols, property, true))
						return true;
				return false;
			}

		};
		return (SavePointRestoreable) program.findDeviceInAll(c);
	}

	public SavePoint copyState() {
		SavePoint r = new SavePoint(program);
		r.withArgs = withArgs;
		r.file = file;

		r.lastUpdate = this.lastUpdate;

		for (String s : this.items)
			r.items.add(s);

		for (String k : this.valuesToSave.keySet())
			r.valuesToSave.put(k, this.valuesToSave.get(k));

		for (String s : this.doneBlocks)
			r.doneBlocks.add(s);

		return r;
	}

	public static SavePoint newSavePoint(DBS program, boolean withArgs) {
		return new SavePoint(program, withArgs);
	}

	public boolean isWithArgs() {
		return withArgs;
	}

	public File getFile() {
		if (file == null) {
			String fileName = generateFileName(program, withArgs);
			file = new File(fileName);
		}
		return file;
	}

	public boolean fileExists() {
		return getFile().exists();
	}

}