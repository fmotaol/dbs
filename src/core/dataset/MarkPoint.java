package core.dataset;

public interface MarkPoint {
	
	/* Deve retornar: 
		-n se MarkPoint é anterior a record;
		 0 se MarkPoint e record são iguais
		+n se MarkPoint é posterior a record;
	*/ 
	public int compareTo(Record record);

}
