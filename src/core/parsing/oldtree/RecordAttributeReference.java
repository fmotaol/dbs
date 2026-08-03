package core.parsing.oldtree;

import core.performer.Context;

public class RecordAttributeReference extends AbstractVarReference {

	public enum SystemRefType {
		ROWID, FIELDCOUNT, FIELDNAME
	};

	@Override
	public String concretize(Context context) {
		throw new RuntimeException("ainda não implementado");
		// TODO Auto-generated method stub
	}

}
