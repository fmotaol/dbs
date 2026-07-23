package core;

import java.util.List;

import core.performer.Performer;

public class Engine {
	
	public static void print(String... ss) {
		for (String s : ss) {
			System.out.print(s);
		}
	}

	public static void println(String... ss) {
		for (String s : ss) {
			System.out.print(s);
		}
		System.out.println();
		
	}

//	@Deprecated
//	public static void print_Old(int indent, String... s) {
//		for (int i = 0; i < indent; i++) {
//			println("  ");
//		}
//		for (int i = 0; i < s.length; i++) {
//			if (s[i] != null)
//				System.out.print(s[i]);
//		}
//	}

	public static void println() {
		System.out.println();
	}

	protected static void nameDevice(String baseName, Performer device, List<? extends Performer> list) {
		int sz = list.size();
		if (sz == 0) {
			device.setSimpleName(baseName);
			return;
		}

		if (sz == 1) {
			list.get(0).setSimpleName(baseName + 1);
			device.setSimpleName(baseName + 2);
		}

		device.setSimpleName(baseName + (sz + 1));
	}


}
