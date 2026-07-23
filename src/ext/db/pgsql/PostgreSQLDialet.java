package ext.db.pgsql;

import java.sql.SQLException;

import org.postgresql.jdbc.PgArray;

import core.sql.Language;

public class PostgreSQLDialet extends Language {

	static final String DEFAULT_PG_INSERT_TEMPLATE = 
			"insert into @#tablename (@#fieldnames) values (@*) on conflict do nothing;";
	
	static final String DEFAULT_PG_UPSERT_TEMPLATE = 
			"insert into @#tablename (@#fieldnames) values (@*) on conflict (@#tablepk) do update set @*=;";
		

	@Override
	public String getInsertTemplate() {
		return DEFAULT_PG_INSERT_TEMPLATE;
	}


	@Override
	public String getUpsertTemplate() {
		return DEFAULT_PG_UPSERT_TEMPLATE;
	}

	@Override
	protected String valueAsSQLForSpecialTypes(Object value) {
		if (value instanceof PgArray) {
			PgArray a = (PgArray) value;
			Object[] ar;
			try {
				ar = (Object[]) a.getArray();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			return arrayAsSQL(ar);
		}
		
		throw new RuntimeException("Tipo não suportado: " + value.getClass().getName());
	}


}
