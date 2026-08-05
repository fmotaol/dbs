package core.parsing.string;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeHistory {

	private Map<String, String> replacements = new HashMap<>();

	private ArrayList<String> additions = new ArrayList<>();

	private ArrayList<String> exclusions = new ArrayList<>();

	public Map<String, String> getReplacements() {
		return Collections.unmodifiableMap(replacements);
	}

	public List<String> getAdditions() {
		return Collections.unmodifiableList(additions);
	}

	public List<String> getExclusions() {
		return Collections.unmodifiableList(exclusions);
	}

	void add(String s) {
		additions.add(s);
	}

	void remove(String s) {
		exclusions.add(s);
	}

	void replace(String from, String to) {
		replacements.put(from, to);
	}

}

