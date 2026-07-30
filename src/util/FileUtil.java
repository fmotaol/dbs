package util;

import java.text.SimpleDateFormat;
import java.util.Date;

import core.DBS;
import core.args.Argument;
import core.args.Argument.Origin;

public class FileUtil {

	private static final String FILENAME_PROHIBITED_CHARS = "[^a-zA-Z0-9_\\-\\.#=]";

//	static String prohibited = "\\/|?<>*:\"";

	public static String removeFileNameProhibitedChars(String r) {
		return r.replaceAll(FILENAME_PROHIBITED_CHARS, "");
	}

	public static String generateFileNameForArguments(DBS program, String separator, String ext, boolean argNames,
			boolean defaultArgs) {
		String filename = program.getDBSFileName();
		String args = getArgsStringForFileName(program, separator, argNames, defaultArgs);
		String r = filename;
		if (!"".equals(args))
			r += separator + args;

		r = removeFileNameProhibitedChars(r);
		return r + ext;
	}

	private static String getArgsStringForFileName(DBS program, String separator, boolean argNames,
			boolean defaultArgs) {
		String r = "";
		for (Argument a : program.arguments) {
			if (!defaultArgs && a.getOrigin() == Origin.DEFAULT)
				continue;
			String s = "";
			if (argNames)
				s += a.getName() + "=";
			s += a.getValue();
			if (!"".equals(r))
				r += separator;
			r += s;
		}
		return r;
	}

	public static String generateFileNameWithTime(DBS program, String ext, Date time) {
		String filename = program.getDBSFileName();
		SimpleDateFormat sdf = new SimpleDateFormat(" yyyy-MM-dd HH-mm-ss");
		String r = filename + " " + sdf.format(time) + " " + Maths.random(100000) + ext;
		return r;
	}

	public static String fileExt(String file) {
		int i = file.lastIndexOf('.');
		if (i > 0)
			return file.substring(i);
		else
			return "";
	}

}
