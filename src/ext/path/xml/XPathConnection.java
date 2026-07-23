package ext.path.xml;

import ext.path.PathConnection;
import ext.path.PathProcess;

public class XPathConnection extends PathConnection {

	@Override
	protected PathProcess newPathProcess(String sql) {
		return new XPathProcess(sql);
	}

	@Override
	public String getId() {
		return "xpath";
	}

}
