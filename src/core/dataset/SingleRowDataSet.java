package core.dataset;

public abstract class SingleRowDataSet extends AbstractDataSet {

	private int currentRowIndex;

	public SingleRowDataSet() {
		resetCurrentRow();
	}
	
	protected void resetCurrentRow() {
		currentRowIndex = -1;
	}

	@Override
	public boolean internalNext() {
		if (!isActive()) {
			return false;
		}

//		saveAsPreviousRecord(currentRecord);

		updateRecordBuffers(false);
		currentRowIndex++;
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
	public void close() {
		// dir = null;
	}

	@Override
	public int getRowId() {
		return currentRowIndex + 1;
	}

	@Override
	public boolean isActive() {
		return currentRowIndex < 0;
	}

	@Override
	public boolean isBeforeFirst() {
		return currentRowIndex == -1;
	}

//	@Override
//	public boolean hasFinished() {
//		return currentRowIndex >= rows.length - 1;
//	}

}
