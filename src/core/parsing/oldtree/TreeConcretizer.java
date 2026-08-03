package core.parsing.oldtree;

import core.parsing.Concretizer;
import core.parsing.replace.MultiOpTranslator;
import core.parsing.replace.StringConcretizer;
import core.performer.Performer;
import util.Util;

public class TreeConcretizer extends Concretizer {
	
	Node root;

	public TreeConcretizer(Performer performer) {
		super(performer);
	}

	public void compile(String templateSQL) {
		throw new RuntimeException("ainda não implementado");
		//TODO implementar
	}
	
	static final String regexFieldsAll = "(?<fall>@\\*(?<fallpk>\\-pk)?)";

	static final String regexFieldsAssignment = "(?<fass>@\\*(?<fasspk>\\-pk)?)";

	static final String regexFieldsAssignmentConjunction = "(?<fand>@\\*=(?<fandpk>\\-pk)?)";

	static final String regexFieldsPK = "(?<fpk>@\\*=pk)";

	static final String regexFieldReferenceByName = "(?<fbyname>@" + Util.regexIdentifier + ")(\\{?<level>.*\\})?";

	static final String regexFieldReferenceByIndex = "(?<fbyindex>@field\\[(?<findex>\\.*)\\])" + Util.regexIdentifier + "(\\{?<level>.*\\})?";

	static final String regexVar = StringConcretizer.regexVar;
	
	static final String regexMultiOp = MultiOpTranslator.regexMultiOp;

//	static final String regexSubQuery = StringConcretizer.regexSubQuery;

}
