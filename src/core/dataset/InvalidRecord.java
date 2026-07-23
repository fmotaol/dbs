package core.dataset;

public class InvalidRecord extends Record {

	public InvalidRecord(DataSet source) {
		super(source, -100);
	}

	@Override
	public Object getValue(int fieldIndex) {
		throw new RuntimeException("Registro inválido");
	}

	@Override
	public boolean valid() {
		return false;
	}

	@Override
	public String getAlias() {
		throw new RuntimeException("Registro inválido");
	}

}
