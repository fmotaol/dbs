package ext.db.pgsql;

import java.sql.Connection;
import java.sql.Statement;

import ext.db.DriverSupport;

//Corresponde ao Driver oficial, ex: postgresql-42.7.7.jar
public class PostgresqlDriverSupport extends DriverSupport {

	@Override
	public void setupWarningListener(Connection connection) {
	}

	public void assignNoticeListener(Statement statement) {
	}

}
