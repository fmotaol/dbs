package ext.db.mysql;

import core.sql.Language;

public class MySQLDialet extends Language {

	static final String DEFAULT_MYSQL_UPSERT_TEMPLATE = 
			"replace into @#tablename values (@*);";

	@Override
	public String getUpsertTemplate() {
		return DEFAULT_MYSQL_UPSERT_TEMPLATE;
	}

}
