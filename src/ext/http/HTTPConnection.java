package ext.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;

import core.dataset.DataSet;
import core.performer.Batch;
import core.performer.Context;
import core.performer.DBSConnection;
import core.performer.Performer;
import core.performer.Result;
import core.performer.TargetPerformer;
import core.sql.Language;

public class HTTPConnection extends DBSConnection {

//	private HttpClient client;

	private HttpURLConnection connection;

	@Override
	public DataSet query(String url, Performer invoker) {
		String resp = request(url);
		return new HttpResponseDataSet(resp);
	}

//	private HttpResponse<String> request(String url) {
//		url = url.trim();
//		if (client == null)
//			client = HttpClient.newHttpClient();
//		HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
//		HttpResponse<String> resp;
//		try {
//			resp = client.send(req, HttpResponse.BodyHandlers.ofString());
//		} catch (IOException | InterruptedException e) {
//			throw new RuntimeException(e);
//		}
//		return resp;
//	}

//	private String request(String urlString) {
//		StringBuilder content;
//		try {
//			URL url = new URL(urlString);
//
//			connection = (HttpURLConnection) url.openConnection();
//			connection.setRequestMethod("GET");
//
//			int status = connection.getResponseCode();
//			System.out.println("Status: " + status);
//
//			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//			String inputLine;
//			content = new StringBuilder();
//
//			while ((inputLine = in.readLine()) != null) {
//				content.append(inputLine);
//			}
//
//			in.close();
//
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		connection.disconnect();
//
//		return content.toString();
//	}

	public String request(String url) {
		try {
			return request0(url);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String request0(String urlString) throws IOException {
		URL url = new URL(urlString);
		connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		try (InputStream inputStream = connection.getInputStream();
				PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, 3);
				BufferedReader reader = new BufferedReader(new InputStreamReader(pushbackStream, "UTF-8"))) {

			// Detecta e remove BOM
			byte[] bom = new byte[3];
			int bytesRead = pushbackStream.read(bom);
			if (bytesRead != 3 || bom[0] != (byte) 0xEF || bom[1] != (byte) 0xBB || bom[2] != (byte) 0xBF) {
				pushbackStream.unread(bom, 0, bytesRead);
			}

			StringBuilder content = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line);
			}

			return content.toString();
			
		} finally {
			connection.disconnect();
		}
	}

	@Override
	public void setAutoCommit(Boolean autoCommit) throws SQLException {
		// sem efeito
	}

	@Override
	public Result execute(String url, Performer invoker, Context context) {
		String resp = request(url);
		Result r = parseResult(resp);
		return r;
	}

	private Result parseResult(String response) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public Batch createBatch(Performer performer) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void defaultStartImportingData(TargetPerformer target) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void defaultImportRow(TargetPerformer target, Context context) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void defaultEndImportingData(TargetPerformer target) {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void reconnect() {
		// TODO Auto-generated method stub
		throw new RuntimeException("ainda não implementado");
	}

	@Override
	public void close() {
		connection = null;
	}

	@Override
	public Language getLanguage() {
		return new HTTP();
	}

	@Override
	public String getId() {
		return "http";
	}

}
