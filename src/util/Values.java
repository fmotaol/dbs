package util;

public class Values {
	
	public static int compareValues(Object a, Object b) {
		
		if (a == null || b == null)
			throw new RuntimeException("Valores nulos não podem ser comparados");

		int r;
		
		try {
			
			r = compareAsNumbers(a, b);
			
			return r;
			
		} catch (Exception e) {}
		
		return compareValues((Comparable) a, (Comparable) b);
	}

		
	private static int compareAsNumbers(Object a, Object b) {
		if (a instanceof String)
			a = convertIntoNumber((String) a);
		if (b instanceof String)
			b = convertIntoNumber((String) b);

		return compareNumbers((Number) a, (Number) b);
	}


	private static Number convertIntoNumber(String a) {
		return convertIntoFloat(a);
	}


	private static Float convertIntoFloat(String a) {
		return Float.parseFloat(a);
	}

	static Integer convertIntoInt(String a) {
		return Integer.parseInt(a);
	}

	private static int compareNumbers(Number a, Number b) {
		float fa = a.floatValue();
		float fb = b.floatValue();
		return compare(fa, fb);
	}


	private static int compare(float fa, float fb) {
		if (fa == fb)
			return 0;
		if (fa > fb)
			return +1;
		if (fa < fb)
			return -1;
		
		throw new RuntimeException("Erro interno");
	}


	public static int compareValues(Comparable a, Comparable b) {
		int r = a.compareTo(b);
		return r;
	}


}
