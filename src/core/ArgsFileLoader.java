package core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import core.Argument.Origin;

public class ArgsFileLoader {

	private File file;

	private DBS program;

	public ArgsFileLoader(DBS program, File file) {
		super();
		this.program = program;
		this.file = file;
	}

	public void loadContent() throws IOException {
		System.out.println("Carregando argumentos do arquivo " + file);
		FileReader fr = new FileReader(file);
		BufferedReader r = new BufferedReader(fr);
		int rownum = 1;
		try {
			while (r.ready()) {
				String row = r.readLine();
				parseArgsFileRowContent(rownum, row);
				rownum++;
			}
			System.out.println();
		} finally {
			r.close();
		}
	}

	private void parseArgsFileRowContent(int rownum, String row) {
		row = row.trim();
		if (row.startsWith("//"))
			return;
		String[] ss = row.split("=", 2);
		if (ss.length > 2)
			throw new RuntimeException("Erro no conte�do do arquivo .args");

		System.out.println("Carregando argumento " + row);

//		if (ss.length == 1) { // arg by index
//			program.addMainArg(ss[0]);
//		} else {

		String arg = ss[0];
		Argument a = program.getArgByName(arg);
		if (a == null)
//			a = program.createArg(arg);
			throw new RuntimeException("Argumento não encontrado: " + ss[0]);
		if (a.origin == null)
			a.setValue(ss[1].trim(), Origin.ARG_FILE);
		else
			System.out.println(
					"AVISO: argumento " + a.getName() + " do arquivo .args ignorado. Já carregado de " + a.origin);

//			XX
//			program.addMainArg(ss[1].trim());
//		}
	}

}
