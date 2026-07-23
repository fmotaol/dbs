package core.file;

import java.io.File;

import core.dataset.AbstractDataSet;
import core.dataset.Header;

public class FileListDataSet extends AbstractDataSet {

	class FileRef {
		File file;
		public FileRef(File file) {
			super();
			this.file = file;
		}
		
		String getFileName() {
			return file.getName();
		}
		
		String getFilePath() {
			return file.getAbsolutePath();
		}
		
		String getSimpleName() {
			String fileName = file.getName();
			int lastDotIndex = fileName.lastIndexOf('.');
			if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
				return fileName.substring(0, lastDotIndex);
			}
			return fileName;
		}
		
		String getFileExt() {
			String fileName = file.getName();
			int lastDotIndex = fileName.lastIndexOf('.');
			if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
				return fileName.substring(lastDotIndex + 1);
			}
			return "";
		}
	}
	
	private FileRef[] rows;

	private int currentRowIndex;

	private static String[] fieldNames = { "filename", "filepath", "simplefilename", "fileext" };

	public FileListDataSet(String dir) {
		super();
		openDir(dir);
	}

	private void openDir(String dir) {
		File[] fs = FileSearch.searchFiles(dir);
		rows = new FileRef[fs.length];
		for (int i = 0; i < fs.length; i++) {
			rows[i] = new FileRef(fs[i]);
		}
		currentRowIndex = -1;
	}

	@Override
	public boolean internalNext() {
		if (!isActive()) {
			return false;
		}

//		saveAsPreviousRecord(currentRecord);
		
		currentRowIndex++;
		updateRecordBuffers(false);
		return true;
	}

	@Override
	public boolean hasNext() {
		if (!isActive()) {
			return false;
		}

		return true;
	}

	@Override
	public int getFieldCount() {
		return 4;
	}

	@Override
	public String getFieldName(int index) {
		return fieldNames[index];
	}

	@Override
	public void close() {
		// dir = null;
	}

	@Override
	public int getRowId() {
		return currentRowIndex + 1;
	}

	@Override
	public boolean isActive() {
		return currentRowIndex < rows.length - 1;
	}

	@Override
	public String[] getFieldNames() {
		return fieldNames;
	}

	@Override
	public Object readValue(int fieldIndex) {
		FileRef r = rows[currentRowIndex];
		String field = getFieldName(fieldIndex);
		if (field.equalsIgnoreCase("filename"))
			return r.getFileName();
		if (field.equalsIgnoreCase("filepath"))
			return r.getFilePath();
		if (field.equalsIgnoreCase("simplefilename"))
			return r.getSimpleName();
		if (field.equalsIgnoreCase("fileext"))
			return r.getFileExt();
		throw new RuntimeException("Erro Interno");
	}

	@Override
	protected void putFields(Header header) {
		for (int i = 0; i < fieldNames.length; i++) {
			header.addField(fieldNames[i], "text");
		}
	}

	@Override
	public boolean isBeforeFirst() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

}
