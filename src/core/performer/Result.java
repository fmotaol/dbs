package core.performer;

import core.dataset.DataSet;

public class Result {
	
	public Result(int affectedRows) {
		super();
		if (affectedRows < 0)
			affectedRows = 0;
//			throw new RuntimeException("Número de linhas afetadas negativo");
		this.affectedRows = affectedRows;
	}

	@Override
	public String toString() {
		return "Comando executado (" + affectedRows + " linhas afetadas)";
	}

	public Result(int affectedRows, DataSet dataSet) {
		this.dataSet = dataSet;
	}

	private int affectedRows = 0;
	
	private DataSet dataSet;

	public int getAffectedRows() {
		return affectedRows;
	}

	public DataSet getDataSet() {
		return dataSet;
	}
	
}
