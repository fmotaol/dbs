package ext.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;

import ext.db.db2.DB2DriverSupport;
import ext.db.pgsql.PgsqlDriverSupport;
import ext.db.pgsql.PostgresqlDriverSupport;

public class DriverSupport {

	public void setupWarningListener(Connection connection) {
	}


	public static DriverSupport createSupport(String driver, String url) {
		if (driver != null) {
			if (driver.equals("com.ibm.db2.jcc.DB2Driver"))
				return new DB2DriverSupport();

			return new DefaultDriverSupport();
		}

		if (url == null)
			throw new RuntimeException("Driver nem URL informados");

		if (url.startsWith("jdbc:postgresql"))
			return new PostgresqlDriverSupport();
		
		if (url.startsWith("jdbc:pgsql"))
			return new PgsqlDriverSupport();

		if (url.startsWith("jdbc:db2"))
			return new DB2DriverSupport();

		return new DefaultDriverSupport();
	}


	@Deprecated
	public void checkWarnings(Connection connection) {
		try {
			//System.out.println("Checando warnings...");
			if (connection == null)
				return;
			
			SQLWarning w = connection.getWarnings();
			if (w == null)
				return;

			System.out.println(w.getMessage());

			while (w != null) {
				System.out.println(w.getMessage());
				w = w.getNextWarning();
			}

			connection.clearWarnings();

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

}
