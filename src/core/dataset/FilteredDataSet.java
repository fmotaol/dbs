package core.dataset;

import java.util.function.Predicate;

public class FilteredDataSet extends ProxyDataSet {

	private Predicate<Record> filter;

	public FilteredDataSet(DataSet target) {
		super(target);
	}

	public Predicate<Record> getFilter() {
		return filter;
	}

	public void setFilter(Predicate<Record> filter) {
		this.filter = filter;
	}

	@Override
	public boolean next() {
		boolean n;
		do {
			n = target.next();
			if (!n)
				return false;

			Record r = currentRecord();
			if (filter.test(r))
				return true;
		} while (n);
		return n;
	}

	@Override
	public boolean hasNext() {
		if (!target.hasNext())
			return false;
		MarkPoint mp = generateMarkPoint();
		boolean r = next();
		recoverMarkPoint(mp);
		return r;
	}

	@Override
	public boolean previous() {
		boolean p;
		do {
			p = target.previous();
			if (!p)
				return false;

			Record r = currentRecord();
			if (filter.test(r))
				return true;
		} while (p);
		return p;
	}

	@Override
	public boolean hasPrevious() {
		if (!target.hasPrevious())
			return false;
		MarkPoint mp = generateMarkPoint();
		boolean r = previous();
		recoverMarkPoint(mp);
		return r;
	}

	@Override
	public Record currentRecord() {
		return target.currentRecord();
	}

	@Override
	public int getRowId() {
		return target.getRowId();
	}

	@Override
	public Record previousRecord() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public Object readValue(int fieldIndex) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public Object[] readValues() {
		return target.readValues();
	}

	@Override
	public boolean isBeforeFirst() {
		if (isBeforeFirst())
			return true;
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void beforeFirst() {
		target.beforeFirst();
	}


}
