package core.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;

import core.dataset.Header;
import core.dataset.SingleRowDataSet;

public class FilePropsDataSet extends SingleRowDataSet {

	private BasicFileAttributes fileAttributes;

	private String filePath;

	private static String[] fieldNames = { "filepath", "creation", "modified", "access", "size", "isdir" };
	private static String[] fieldTypes = { "text", "timestamp", "timestamp", "timestamp", "long", "boolean" };

	public FilePropsDataSet(String filePath) {
		super();
		this.filePath = filePath;
		readFileAttrs();
	}

	private void readFileAttrs() {
		Path path = Paths.get(filePath);
		try {
			fileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
		// dir = null;
	}

	@Override
	public String[] getFieldNames() {
		return fieldNames;
	}

	@Override
	public Object readValue(int fieldIndex) {

//		if ((fieldIndex < 0) || (fieldIndex > 3))
//			throw new RuntimeException("Campo inexistente");

		String name = getFieldName(fieldIndex + 1);
		if (name.equals("filepath"))
			return filePath;
		if (name.equals("creation"))
			return toDate(fileAttributes.creationTime());
		if (name.equals("modified"))
			return toDate(fileAttributes.lastModifiedTime());
		if (name.equals("access"))
			return toDate(fileAttributes.lastAccessTime());
		if (name.equals("size"))
			return fileAttributes.size();
		if (name.equals("isdir"))
			return fileAttributes.isDirectory();

		throw new RuntimeException("Campo desconhecido: " + name);
	}

	private Object toDate(FileTime time) {
		return Date.from(time.toInstant());
	}

	@Override
	protected void putFields(Header header) {
		for (int i = 0; i < fieldNames.length; i++) {
			header.addField(fieldNames[i], fieldTypes[i]);
		}
	}

}
