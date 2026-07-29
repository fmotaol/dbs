package core.jdbc;

import java.sql.Statement;

import core.performer.DBSConnection;

class WarningChecker {

	JDBCConnection connection;
	Statement statement;
	boolean done = false;

	public WarningChecker(JDBCConnection connection, Statement statement) {
		this.connection = connection;
		this.statement = statement;

//			ExecutorService service = getWarningExecutorService();

		connection.parallel.run(() -> {
			do {
				if (this.connection == null)
					return;

				// System.out.println("Executando...");
				DBSConnection.sleep(200);
				this.check();

			} while (true);
//					} while (!isReady() && !done);
		});

	}

	public void check() {

		connection.driverSupport.checkWarnings(statement);
		// driverSupport.checkWarnings(connection);

	}

	public void finish() {
		this.done = true;
	}

	protected void shutdown() throws Throwable {
		finish();
	}
}
