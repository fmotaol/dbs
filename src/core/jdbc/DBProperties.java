package core.jdbc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public class DBProperties {

	public String id;
	public String driver;
	public String url;
	public Integer port;
	public String user;
	public String password;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public static DBProperties newFromFile(String filePath) {
		return newFromFile(filePath, null);
	}

	public static DBProperties newFromFile(String filePath, Map<String, String> otherProperties) {

		DBProperties r = new DBProperties();
		r.id = filePath;

		BufferedReader reader;
		try {
			reader = new BufferedReader(newFileReader(filePath));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		String line = null;

		try {
			do {
				line = readLine(reader);
				parseLine(line, r, otherProperties);
				
			} while (line != null);
			return r;
		} finally {
			close(reader);
		}
	}

	public static DBProperties newFromString(String string, String splitter) {
		return newFromString(string, splitter, null);
	}

	public static DBProperties newFromString(String string, String splitter, Map<String, String> otherProperties) {

		DBProperties r = new DBProperties();
		r.id = string;

		String[] ss = string.split(splitter);
		
		for (String s : ss) {
			parseLine(s.trim(), r, otherProperties);
		}

		if (r.url == null)
			throw new RuntimeException("Conexão inválida: " + string);
		
		return r;
	}

	private static void parseLine(String line, DBProperties r, Map<String, String> otherProperties) {
		if (line == null)
			return;
		line = line.trim();

		if (line.isEmpty())
			return;

		if (line.startsWith("driver")) {
			String ss[] = line.split("=");
			r.driver = ss[1];
			return;
		}

		if (line.startsWith("url")) {
			String ss[] = line.split("=");
			r.url = ss[1];
			return;
		}

		if (line.startsWith("user")) {
			String ss[] = line.split("=");
			r.user = ss[1];
			return;
		}

		if (line.startsWith("password")) {
			String ss[] = line.split("=");
			r.password = ss[1];
			return;
		}

		if (line.startsWith("port")) {
			String ss[] = line.split("=");
			r.port = Integer.parseInt(ss[1]);
			return;
		}

		if (line.startsWith("reconnect_on_error")) {
			throw new RuntimeException("Propriedade descontinuada");

//			String ss[] = line.split("=");
//			r.reconnectOnError = ss[1].trim();
//			continue;
		}

		if (otherProperties != null) {
			String ss[] = line.split("=");
			if (ss.length == 0)
				return;
			String key = ss[0];
			if (ss.length > 2)
				throw new RuntimeException("Inconsistência de propriedade: " + key);
			String value = null;
			if (ss.length == 2)
				value = ss[1];
			otherProperties.put(key, value);
		}
	}

	private static void close(BufferedReader reader) {
		try {
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String readLine(BufferedReader reader) {
		try {
			return reader.readLine();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}

	private static Reader newFileReader(String filePath) {
		try {
			return new FileReader(filePath);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

}
