package core.parsing.oldtree;

import core.dataset.Field;
import core.performer.Context;

public abstract class FieldReference extends AbstractFieldReference {

	private Integer level;

	private String fieldId;

	private boolean isNative = false;

	@Override
	public String concretize(Context context) {
		Field f = null;
		if (level == null)
			f = context.getField(fieldId);
		else
			f = context.getField(fieldId, level);

		throw new RuntimeException("ainda não implementado");
		
//		Language lang = null;
//		return record.valueAsString(f, isNative, lang);
	}

}
