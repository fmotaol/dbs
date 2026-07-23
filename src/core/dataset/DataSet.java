package core.dataset;

import core.performer.Context;

public interface DataSet {

	public boolean next();
	
	public boolean hasNext();

	public boolean previous();

	public boolean hasPrevious();

	public Record currentRecord();

	public int getFieldCount();

	public String getFieldName(int index);

	public void close();

	public int getRowId();

	public Record previousRecord();

	public boolean isActive();

	public String[] getFieldNames();

	public default int getIndexOfFieldName(String name) {
		for (int i = 1; i <= getFieldCount(); i++) {
			String fn = getFieldName(i);
			if (fn.equalsIgnoreCase(name))
				return i;
		}
		throw new RuntimeException("Campo desconhecido: " + name);
	}

	public Object readValue(int fieldIndex);

	public default Object[] readValues() {
		Object[] r = new Object[getFieldCount()];
		for (int i = 0; i < r.length; i++) {
			r[i] = readValue(i);
		}
		return r;
	}

	public boolean isBeforeFirst();

	public void beforeFirst();

	public InvokerData getInvokerData();

	public static class InvokerData {
		
		private DataSet owner;
		
		private String command;

		private Context context;

		private String alias;

		public InvokerData(DataSet owner) {
			super();
			this.owner = owner;
		}

		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}

		public Context getContext() {
			return context;
		}

		public void setContext(Context context) {
			this.context = context;
		}

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

	}

	public Header getHeader();

	public void setInvokerData(String sql, Context context, String alias);

	public default MarkPoint generateMarkPoint() {
		return new RowIdMarkPoint(getRowId());
	}

	public default void recoverMarkPoint(MarkPoint mp) {
		int cmp;
		boolean r = true;
		do {
			cmp = mp.compareTo(currentRecord());
			if (cmp == 0)
				return;
			if (cmp < 0)
				r = next();
			if (cmp > 0)
				r = previous();
		} while(r);
		
		throw new RuntimeException("Não foi possível restaurar o MarkPoint " + mp);
	}


}
