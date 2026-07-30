package core.jdbc;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import core.DBS;
import core.performer.DBSConnection;
import util.threads.Parallelizer;

class WarningChecker {

	JDBCConnection connection;
	Statement statement;
	boolean done = false;

	public WarningChecker(JDBCConnection connection, Statement statement) {
		this.connection = connection;
		this.statement = statement;
		Parallelizer par = DBS.parallelizer();
		par.run(() -> {
			do {
				if (this.connection == null)
					return;

				DBS.sleep(200);
			
				if (par.finished())
					return;
				
				if (!statementIsActive())
					return;
				
				this.check();

			} while (!done);
		});

	}

	private boolean statementIsActive() {
		try {
			return !statement.isClosed();
		} catch (SQLException e) {
			return false;
		}
	}

	public void check() {

		try {
			// System.out.println("Checando warnings...");
			if (statement == null)
				return;

			SQLWarning w = statement.getWarnings();
			if (w == null)
				return;

			while (w != null) {
				System.out.println(w.getMessage());
				w = w.getNextWarning();
			}

			statement.clearWarnings();

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

	}

	public void finish() {
		this.done = true;
	}

	protected void shutdown() throws Throwable {
		finish();
	}
}
