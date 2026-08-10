package core.performer;

import java.sql.SQLException;

import core.sql.Language;

public class TableData {

	private Performer performer;

	private String tableName;

	public TableData(Performer performer) {
		super();
		this.performer = performer;
	}

	public Performer getPerformer() {
		return performer;
	}

	public String[] getFieldsToFilter(Context context) {
		DBSConnection c = getConnection();
		// if (c.isInsertOrUpdateCommand(getTemplateCommand()))
		if (hasTableName(context))
			return c.getFieldsByTable(getTableName(context));
		else
			return null;

	}

	private boolean hasTableName(Context context) {
		if (tableName != null)
			return true;

		String templateCommand = performer.getTemplateCommand(context);
		Language l = getConnection().getLanguage();
		if (l.isInsertOrUpdateCommand(templateCommand))
			return true;

		return false;
	}

	public String getTableName(Context context) {
		Language l = getConnection().getLanguage();
		if (tableName == null) {
			String templateCommand = performer.getTemplateCommand(context);
			tableName = l.inferTableName(templateCommand);
		}
		return tableName;
	}

	public String[] getTableFields(Context context) {
		DBSConnection c = getConnection();
		String t = getTableName(context);
		return c.getFieldsByTable(t);
	}

	public String[] getPrimaryKeyFields(Context context) {
		try {
			DBSConnection c = getConnection();
			String t = getTableName(context);
			return c.getPrimaryKeyFields(t);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public DBSConnection getConnection() {
		return performer.getConnection();
	}

	// public String[] getFieldsToFilter(String sql, DataSet source, String
	// recordName)
	// {
	// String[] r = null;
	// if (isInsertOrUpdateCommand(sql))
	// if (ignoreUnknownFields)
	// r = getTableFields(sql, source);
	// return r;
	// }

	void setDefaultImport(String templateSQL) {
		templateSQL = templateSQL.trim();
		if (templateSQL == null)
			return;
		tableName = extractTableName(templateSQL);
	}

	private String extractTableName(String templateSQL) {
		if (TargetPerformer.isDefaultImport(templateSQL)) {
			int length = TargetPerformer.DEFAULT_IMPORT.length();
			templateSQL = templateSQL.substring(length);
			if (templateSQL.endsWith(";"))
				templateSQL = templateSQL.substring(0, templateSQL.length() - 1);

			tableName = templateSQL.trim();
			if (tableName.isEmpty())
				tableName = null;
			return tableName;
		}
		return null;
//		throw new RuntimeException("ainda não implementado");
	}

}
