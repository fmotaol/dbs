package core.join;

import util.Values;

public interface KeySource {

	public Object[] previousDifferentKey();

	public Object[] previousKey();

	public Object[] currentKey();

	default boolean missedCurrentKeyIn(KeySource other) {

		Object[] tk = currentKey();
		Object[] ok = other.currentKey();

		if (isNullKey(tk))
			throw new RuntimeException("Erro interno");

		int sd = commonSortDirection(other);

		if (!isNullKey(ok)) {
			int cmpo = compareKeys(tk, ok);
			if (cmpo == 0) // chaves iguais = match
				return false;

			if (sd == 0) // sentido indefinido. Não dá pra afirmar.
				return false;

			if (cmpo * sd > 0) // registro do futuro, não há como avaliar aqui
				return false;
//				throw new RuntimeException("Comparação de registros chamada fora de posição");
		}

		// a partir daqui, ou ok é nulo, ou cmpo * sd indica que tk < ok

		Object[] pk = other.previousDifferentKey();
		if (pk == null) // ok é o primeiro registro, ou ok é nulo - nos dois casos, miss=true
			return true;

		int cmpp = compareKeys(tk, pk);
		if (cmpp == 0) // existe, e é pk
			return false;

		if (ok != null) {
			if (cmpp * sd < 0) // tk < pk, e portando não dá pra avaliar
				throw new RuntimeException("Comparação de registros chamada fora de posição");

			return true; // porque pk < tk < ok
		} else
			return true; //porque tk não existe no dataset other 

	}

	default boolean missedCurrentKeyIn_Old(KeySource other) {
		int cck = this.compareCurrentKey(other);
		if (cck == 0)
			return false; // match

		int sd = commonSortDirection(other);
		if (sd == 0)
			return false;

		boolean ohp = other.hasPrevious();
		if (!ohp) {
			if (cck == -sd)
				return true;
		}

		if (cck == sd && !other.hasNext())
			return true;

		if (ohp) {

			// somente se other.previous < this.current < other.current (se FORWARD)
			// ou se other.previous > this.current > other.current (se BACKWARD)

			int ccop = compareKeys(this.currentKey(), other.previousKey());
			if (ccop == 0)
				return false;

			if (cck == -ccop) // significa que this.ck está entre other.pk e other.ck
				return true;
		}

		return false;
	}

	default int commonSortDirection(KeySource other) {
		int a = this.sortDirection();
		int b = other.sortDirection();
		if (a == b)
			return a;
		if (a == 0)
			return b;
		if (b == 0)
			return a;
		throw new RuntimeException("Datasets estão em ordem diferente de chave");
	}

	public boolean hasPrevious();

	public boolean hasNext();

	public boolean next();

	public int sortDirection(); // 1 = forward, -1 = backward, 0 = unknown

	default int compareCurrentKeyOverDirection(KeySource other) {
		int k = compareCurrentKey(other);
		int d = this.commonSortDirection(other);
		return k * d;
	}

	default int compareCurrentKey(KeySource other) {
		Object[] t = currentKey();
		Object[] o = other.currentKey();
		return compareKeys(t, o);
	}

	default boolean matchCurrentKey(KeySource other) {
		Object[] t = currentKey();
		Object[] o = other.currentKey();
		return equalKeys(t, o);
	}

	default boolean equalKeys(Object[] a, Object[] b) {
		return compareKeys(a, b) == 0;
	}

	default int compareKeys(Object[] keya, Object[] keyb) {
		if (keya.length != keyb.length)
			throw new RuntimeException("Chaves possuem quantidade de campos diferentes");

		for (int i = 0; i < keyb.length; i++) {
			Object a = keya[i];
			Object b = keyb[i];
			int r = Values.compareValues(a, b);
			if (r != 0)
				return r;
		}
		return 0;

	}

	default boolean currentKeyIsNull() {
		if (!isDataSetActive())
			return true;
		Object[] vs = currentKey();
		return isNullKey(vs);
	}

	default boolean isNullKey(Object[] vs) {
		for (Object v : vs) {
			if (v == null)
				return true;
		}
		return false;
	}

	default boolean hasDifferentSortDirection(KeySource other) {
		int t = this.sortDirection();
		if (t == 0)
			return false;
		int o = other.sortDirection();
		if (o == 0)
			return false;
		return t != o;
	}

	public boolean isBeforeFirst();

	public int getRowId();

	public boolean isDataSetActive();

	default void seek(Object[] searchKey) {
		if (isBeforeFirst())
			return;
		int sd = sortDirection();
		if (sd == 0)
			return;

		if (sd == 1) {

			Object[] pk = previousKey();
			int cmp = compareKeys(pk, searchKey);
			if (cmp != 0 && cmp == -sd) // o prev é menor que o current, logo está posicionado
				return;

			do {
				if (isBeforeFirst())
					return;

				Object[] ck = currentKey();
				cmp = compareKeys(ck, searchKey);
				if (cmp == 0 || cmp == sd) // se for menor ou igual ao searchKey
					return;

				previous();

			} while (true);

		}
	}

	public boolean previous();

}
