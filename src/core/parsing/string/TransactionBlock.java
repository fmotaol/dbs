package core.parsing.string;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransactionBlock implements Block {

	private StringBlock content;

	public TransactionBlock() {
		content = new StringBlock();
	}

	public TransactionBlock(String s) {
		content = new StringBlock(s);
	}

	@Override
	public String toString() {
		return content.toString();
	}

	public int length() {
		return content.length();
	}

	private Transaction activeTransaction;

	public Transaction newTransaction() {
		return newTransaction(null);
	}

	public Transaction newTransaction(Predicate<String> newValueCriteria) {
		activeTransaction = new Transaction(newValueCriteria);
		return activeTransaction;
	}

	public void append(String s) {
		if (activeTransaction != null)
			activeTransaction.append(s);
		else
			content.append(s);
	}

	public void appendIfNotEmpty(String separator, String s) {
		if (length() > 0)
			append(separator);
		if (s != null)
			append(s);
	}

	public void setLength(int length) {
		if (activeTransaction != null)
			activeTransaction.setLength(length);
		else
			content.setLength(length);
	}

	public void insert(int offset, String s) {
		if (activeTransaction != null)
			activeTransaction.insert(offset, s);
		else
			content.insert(offset, s);
	}

	public void trimToSize() {
		content.trimToSize();
	}

//	public static boolean contains(StringBuilder sb, String s) {
//		boolean r = sb.indexOf(s) >= 0;
//		return r;
//	}

	public boolean contains(String s) {
		return content.contains(s);
	}

	public boolean contains(String s, boolean ignoreCase) {
		return content.contains(s, ignoreCase);
	}

	public StringBuilder getOriginalBuilder() {
		return content.getOriginalBuilder();
	}

	public TransactionBlock copy() {
		TransactionBlock r = new TransactionBlock();
		r.content = this.content.copy();
		return r;
	}

	public String[] find(Pattern pattern) {
		return content.find(pattern);
	}

	public void replace(String from, String to, boolean ignoreCase, boolean onlyFirst) {
		if (activeTransaction != null)
			activeTransaction.replace(from, to, ignoreCase, onlyFirst);
		else
			content.replace(from, to, ignoreCase, onlyFirst);
	}

	public void replace(Pattern pattern, Function<Matcher, String> replacement) {
		content.replace(pattern, replacement);
	}

	public class Transaction {

		public enum State {
			ACTIVE, COMMITED, CANCELLED
		};

		private State state = State.ACTIVE;

		private String originalString;

		private ChangeHistory firstChange = new ChangeHistory(); // I=insert(sk), R=replace(from, sk), D=remove(from)
		private ChangeHistory lastChange = new ChangeHistory(); // I=replace(sk, to), R=replace(sk, to), D=-
		private ChangeHistory finalHistory = new ChangeHistory(); // I=insert (to), R=replace(from, to), D=remove(from)

		private Predicate<String> newValueCriteria;

		public Transaction(Predicate<String> newValueCriteria) {
			this.newValueCriteria = newValueCriteria;
		}

		public Predicate<String> getValueCriteria() {
			return newValueCriteria;
		}

		public boolean manages(String newValue) {
			if (newValueCriteria == null)
				return true;
			return newValueCriteria.test(newValue);
		}

		public void replace(String from, String to, boolean ignoreCase, boolean onlyFirst) {
			finalHistory.replace(from, to);
			if (!newValueCriteria.test(to)) {
				content.replace(from, to, ignoreCase, onlyFirst);
			} else {
				String k = generateScrambleKey();
				content.replace(from, k, ignoreCase, onlyFirst);
				firstChange.replace(from, k);
				lastChange.replace(k, to);
			}
		}

		public void append(String s) {
			finalHistory.add(s);
			if (!newValueCriteria.test(s)) {
				content.append(s);
			} else {
				String k = generateScrambleKey();
				content.append(k);
				firstChange.add(k);
				lastChange.replace(k, s);
			}
		}

		public void setLength(int length) {
			content.setLength(length);
		}

		public void insert(int offset, String s) {
			finalHistory.add(s);
			if (!newValueCriteria.test(s)) {
				content.append(s);
			} else {
				String k = generateScrambleKey();
				content.insert(offset, k);
				firstChange.add(k);
				lastChange.replace(k, s);
			}
		}

		public void commit() {
			Map<String, String> lastHM = lastChange.getReplacements();
			for (String k : lastHM.keySet()) {
				String to = lastHM.get(k);
				content.replace(k, to);
			}
			content.setHistory(finalHistory);
			state = State.COMMITED;
			activeTransaction = null;
		}

		public void rollback() {
			TransactionBlock.this.content = new StringBlock(originalString);
			state = State.CANCELLED;
		}

		public State getState() {
			return state;
		}

		private String generateScrambleKey() {
			String r = null;
			int i = 1;
			do {
				String rand = (Math.random() + "").replace("0.", "");
				r = "##_SCRAMBLE_" + rand + "_SCRAMBLE_##";
				i++;
			} while (lastChange.getReplacements().get(r) != null && i < 500);

			if (i >= 500)
				throw new RuntimeException("Erro interno. Repetição de chave SCRAMBLE");

			return r;
		}

	}

	public ChangeHistory history() {
		return content.history();
	}

}