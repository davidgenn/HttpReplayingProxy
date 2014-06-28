package davidgenn.httpreplayingproxy.proxy;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

public class HttpReplayingProxyTest {

	@Test
	public void test() throws Exception {
		Server server = new HttpReplayingProxy(
				new HttpReplayingProxyConfiguration()
				.urlToProxyTo("http://bbc.co.uk")
				.portToHostOn(8080))
		.start();
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet("http://localhost:8080/sport");
		CloseableHttpResponse proxiedResponse = httpclient.execute(httpGet);
		String content = IOUtils.toString(proxiedResponse.getEntity().getContent());
		server.stop();
	}

}
