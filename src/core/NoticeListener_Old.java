package core;

import java.sql.SQLWarning;

import core.performer.DBSConnection;

@Deprecated
class NoticeListener_Old extends Thread {

	private DBS program;

	public NoticeListener_Old(DBS program) {
		super();
		this.program = program;
	}

	@Override
	public void run() {

		while (true) {
			try {
				for (String id : program.getConnectionIds()) {
					DBSConnection c = program.getConnection(id);
					SQLWarning w = null;
						while (c.hasNotice()) {
						String notice = c.consumeNotice();
							if (w != null)
								System.out.println(notice);

					};

				}

				Thread.sleep(500);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

}
