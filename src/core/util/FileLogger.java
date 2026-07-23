package core.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import core.DBS;
import util.FileUtil;

public class FileLogger implements Logger {

	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	@Override
	public void log(String... strings) {
		String time = "[" + timeFormat.format(new Date()) + "]";
		try {
			writer.write(time);
			for (String s : strings)
				writer.write(s);
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	private File file;

	BufferedWriter writer;

	public FileLogger(DBS program, Date time) {
//		this.program = program;
		String name = generateFileName(program);
		this.file = new File(name);
		try {
			FileWriter fw = new FileWriter(this.file, false);
			writer = new BufferedWriter(fw);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private long lastUpdate = (new Date()).getTime();

	public static String generateFileName(DBS program) {
		return FileUtil.generateFileNameWithTime(program, ".log", new Date());
	}

	public void finish() {
		try {
//			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public File getFile() {
		return file;
	}

	public boolean fileExists() {
		return getFile().exists();
	}

}
