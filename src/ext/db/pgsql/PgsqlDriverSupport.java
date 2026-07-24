package ext.db.pgsql;

import java.sql.Connection;

import ext.db.DriverSupport;

//Corresponde ao Driver avançado, ex: pgjdbc-ng-all-0.8.9.jar
public class PgsqlDriverSupport extends DriverSupport {

	// PGConnection pgConn;

	@Override
	public void setupWarningListener(Connection connection) {

//		pgConn = connection.unwrap(PGConnection.class);

	}

	@Override
	public void checkWarnings(Connection connection) {

	}

}
