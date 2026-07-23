package ext.path.json;


import ext.path.PathConnection;
import ext.path.PathProcess;

public class JsonPathConnection extends PathConnection {

//	private JsonLanguageExt language = new JsonLanguageExt();

	@Override
	public void setAutoCommit(Boolean autoCommit) throws Exception {
	}
	
	@Override
	public String getId() {
		return "jsonpath";
	}

	@Override
	protected PathProcess newPathProcess(String sql) {
		return new JsonPathProcess(sql);
	}

}
