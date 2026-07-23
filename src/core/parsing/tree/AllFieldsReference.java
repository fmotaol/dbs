package core.parsing.tree;

import core.dataset.Field;
import core.dataset.Record;
import core.performer.Context;

public class AllFieldsReference extends AbstractFieldReference {

	@SuppressWarnings("unused")
	private boolean removePrimaryKey = false;

	@Override
	public String concretize(Context context) {
		throw new RuntimeException("ainda não implementado");
		//TODO implementar
//		Field[] fs = record.fieldList(false, false, fieldsToFilter(record));
//		if (removePrimaryKey)
//			fs = removePrimaryKey(fs);
//		String r = Field.sqlValueList(fs, ", ");
//		return r;
	}

	@SuppressWarnings("unused")
	private Field[] removePrimaryKey(Field[] fs) {
		throw new RuntimeException("ainda não implementado");
		//TODO implementar
//		String[] pkFields = performer.getPrimaryKeyFields(sql);
//		fs = Field.removeByFieldName(fs, pkFields);
//		return fs;
	}

	@SuppressWarnings("unused")
	private String[] fieldsToFilter(Record record) {
		throw new RuntimeException("ainda não implementado");
		// TODO Auto-generated method stub
	}

}
