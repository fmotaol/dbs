package ext.http;

import core.dataset.Header;
import core.dataset.SingleRowDataSet;

public class HttpResponseDataSet extends SingleRowDataSet {

	private String content;

	public HttpResponseDataSet(String content) {
		super();
		this.content = content;
	}

	@Override
	public int getFieldCount() {
		return 1;
	}

	@Override
	public void close() {
		// dir = null;
	}

	@Override
	public Object readValue(int fieldIndex) {

		if (fieldIndex != 0)
			throw new RuntimeException("Campo inexistente");

		return content;
	}

	@Override
	protected void putFields(Header header) {
//		header.addField("response", "text");
		header.addField("content", "text");
	}

}
