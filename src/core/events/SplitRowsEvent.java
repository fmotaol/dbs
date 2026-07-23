package core.events;

import core.dataset.DataSet;
import core.parsing.replace.StringConcretizer;
import core.performer.Context;

public class SplitRowsEvent extends Event {

	public SplitRowsEvent(StringConcretizer concretizer) {
		super();
	}

	private int rows;

	@Override
	public void setParams(String params) {
		try {
			rows = Integer.parseInt(params);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Número de linhas de quebra inválido");
		}
	}

	@Override
	public boolean checkEachRow(Context context) {
		boolean exactSize = (context.dataSet.getRowId() % rows == 0);
		return exactSize;
//		if (exactSize && !lastRecord)
//			return true;
//		if (!exactSize && lastRecord)
//			return true;
//		return false;
	}

	@Override
	public boolean isIterableOnRows() {
		return true;
	}

	@Override
	public boolean checkAfterLastRow(DataSet source) {
		return true;
	}

}
