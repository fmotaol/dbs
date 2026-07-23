package core.join;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import core.dataset.DataSet;
import core.dataset.Field;
import core.dataset.Record;
import core.parsing.replace.StringConcretizer;
import core.performer.Context;
import core.performer.SourcePerformer;

public class DataSetKeySource implements KeySource {

	private SourcePerformer source;

	private String[] keyTemplate;

	private Future<DataSet> futureDataSet;

	private int sortDirection = 0;

	private Object[] previousDifferentKey;

	public DataSetKeySource(SourcePerformer source, String[] keyTemplate, Future<DataSet> futureDataSet) {
		super();
		this.source = source;
		this.keyTemplate = keyTemplate;
		this.futureDataSet = futureDataSet;
	}

	@Override
	public Object[] previousKey() {
		Record record = dataSet().previousRecord();
		Object[] r = readFromRecord(record);
		if (!equalKeys(r, currentKey()))
			previousDifferentKey = r;
		return r;
	}

	private DataSet dataSet() {
		try {
			return futureDataSet.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Object[] currentKey() {
		Record record = dataSet().currentRecord();
//		if (record == null)
//			throw new RuntimeException("Registro atual é nulo");
		return readFromRecord(record);
	}

	private Object[] readFromRecord(Record record) {
		Object[] r = new Object[keyTemplate.length];
		if (record == null)
			return r;
		for (int i = 0; i < keyTemplate.length; i++) {
			String k = keyTemplate[i].trim();
			Object o = getValue(record, k);
			r[i] = o;
		}
		return r;
	}

	public Object getValue(Record record, String fieldName) {
		Field f = record.fieldByName(fieldName);
		if (f != null)
			return record.getValue(f);
		if (fieldName.contains("@"))
			throw new RuntimeException("Expressão não reconhecida: " + fieldName);

		StringConcretizer conc = source.getDefaultConcretizer();
		String r = conc.concretizeAll(fieldName, new Context(null, record, dataSet()));
		return r;
	}

//	@Override
//	public Object[] previewNextKey() {
//		Record record = dataSet().previewNextRecord();
//		return readFromRecord(record);
//	}

	@Override
	public boolean hasNext() {
		return dataSet().hasNext();
	}

	@Override
	public boolean next() {
		boolean r = source.defaultDataSetNext(dataSet());
		treatSortDirection();
		return r;
	}

	private void treatSortDirection() {
		if (!hasPrevious())
			return;
		int newd = compareKeys(currentKey(), previousKey());
		if (newd == 0)
			return;
		if (sortDirection == 0) {
			sortDirection = newd;
			return;
		}
		if (newd != sortDirection)
			throw new RuntimeException("DataSet não está ordenado pela chave informada");
	}

	@Override
	public boolean hasPrevious() {
		return dataSet().hasPrevious();
	}

	@Override
	public int sortDirection() {
		return sortDirection;
	}

	@Override
	public boolean isBeforeFirst() {
		return dataSet().isBeforeFirst();
	}

	@Override
	public String toString() {
		String ck = "?";
		try {
			ck = Arrays.toString(currentKey());
		} catch (Exception e) {
			ck = e.getMessage();
		}
		;
		String pk = "?";
		try {
			pk = Arrays.toString(previousKey());
		} catch (Exception e) {
			pk = e.getMessage();
		}
		;
		return "DataSetKeySource [keyTemplate=" + Arrays.toString(keyTemplate) + " currentKey()=" + ck
				+ ", previousKey()=" + pk + ", sortDirection=" + sortDirection + " source=" + source + "]";
	}

	@Override
	public int getRowId() {
		return dataSet().getRowId();
	}

	@Override
	public boolean isDataSetActive() {
		return dataSet().isActive();
	}

	@Override
	public boolean previous() {
		boolean r = dataSet().previous();
		return r;
	}

	@Override
	public Object[] previousDifferentKey() {
		return previousDifferentKey;
	}

}
