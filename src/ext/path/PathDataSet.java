package ext.path;

import core.dataset.ColumnarDataSet;

public class PathDataSet extends ColumnarDataSet {

	private PathProcess process;
	
	public PathDataSet(PathProcess process) {
		super();
		this.process = process;
	}

	@Override
	public Object readValue(int fieldIndex) {
		Object v = super.readValue(fieldIndex);
		v = process.convertIfNecessary(v);
		return v;
	}


}
