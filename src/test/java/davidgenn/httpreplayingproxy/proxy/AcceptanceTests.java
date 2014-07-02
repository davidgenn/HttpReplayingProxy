package davidgenn.httpreplayingproxy.proxy;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Server;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.fest.assertions.Assertions.*;

public class AcceptanceTests {

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(8080);

	@Test
	public void test_get_is_proxied_with_headers_and_cached() throws Exception {
		// Given
		stubFor(get(urlEqualTo("/verify/this?query=value"))
				.withHeader("My-Header", equalTo("header-value"))
				.willReturn(aResponse()
						.withStatus(200)
						.withBody("<response>Some content</response>")));

		HttpReplayingProxyConfiguration configuration = 
			new HttpReplayingProxyConfiguration()
		.urlToProxyTo("http://localhost:8080")
		.portToHostOn(8585);

		Server server = new HttpReplayingProxy(configuration).start();

		// When
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet("http://localhost:8585/verify/this?query=value");
		httpGet.addHeader("My-Header", "header-value");
		CloseableHttpResponse proxiedResponse = httpclient.execute(httpGet);

		// Then
		assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
		assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
		assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached")).isNull();
		proxiedResponse = httpclient.execute(httpGet);
		assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
		assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
		assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached").getValue()).isEqualTo("true");

		server.stop();
	}

	@Test
	public void test_post_is_proxied_with_headers_and_cached() throws Exception {
		// Given
		stubFor(post(urlEqualTo("/verify/this"))
				.withHeader("My-Header", equalTo("header-value"))
				.withRequestBody(equalTo("{\"key\":\"value\"}"))
				.willReturn(aResponse()
						.withStatus(200)
						.withBody("<response>Some content</response>")));

		HttpReplayingProxyConfiguration configuration = 
			new HttpReplayingProxyConfiguration()
		.urlToProxyTo("http://localhost:8080")
		.portToHostOn(8585);

		Server server = new HttpReplayingProxy(configuration).start();

		// When
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("http://localhost:8585/verify/this");
		httpPost.addHeader("My-Header", "header-value");
		httpPost.setEntity(new StringEntity("{\"key\":\"value\"}"));
		CloseableHttpResponse proxiedResponse = httpclient.execute(httpPost);

		// Then
		assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
		assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
		assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached")).isNull();
		proxiedResponse = httpclient.execute(httpPost);
		assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
		assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
		assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached").getValue()).isEqualTo("true");

		server.stop();
	}
	
	@Test
	public void test_post_is_proxied_with_headers_and_cached_for_different_bodies() throws Exception {
		// Given
		stubFor(post(urlEqualTo("/verify/this"))
				.withHeader("My-Header", equalTo("header-value"))
				.withRequestBody(equalTo("{\"key\":\"value\"}"))
				.willReturn(aResponse()
						.withStatus(200)
						.withBody("<response>Some content ONE</response>")));
		
		stubFor(post(urlEqualTo("/verify/this"))
				.withHeader("My-Header", equalTo("header-value"))
				.withRequestBody(equalTo("{\"key\":\"otherValue\"}"))
				.willReturn(aResponse()
						.withStatus(200)
						.withBody("<response>Some content TWO</response>")));

		HttpReplayingProxyConfiguration configuration = 
			new HttpReplayingProxyConfiguration()
		.urlToProxyTo("http://localhost:8080")
		.portToHostOn(8585);

		Server server = new HttpReplayingProxy(configuration).start();

		// When
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost1 = new HttpPost("http://localhost:8585/verify/this");
		httpPost1.addHeader("My-Header", "header-value");
		httpPost1.setEntity(new StringEntity("{\"key\":\"value\"}"));
		CloseableHttpResponse proxiedResponse1 = httpclient.execute(httpPost1);
		
		HttpPost httpPost2 = new HttpPost("http://localhost:8585/verify/this");
		httpPost2.addHeader("My-Header", "header-value");
		httpPost2.setEntity(new StringEntity("{\"key\":\"otherValue\"}"));
		CloseableHttpResponse proxiedResponse2 = httpclient.execute(httpPost2);

		// Then
		assertThat(proxiedResponse1.getStatusLine().getStatusCode()).isEqualTo(200);
		assertThat(IOUtils.toString(proxiedResponse1.getEntity().getContent())).isEqualTo("<response>Some content ONE</response>");
		assertThat(proxiedResponse1.getFirstHeader("x-http-replaying-proxy-cached")).isNull();
		
		assertThat(proxiedResponse2.getStatusLine().getStatusCode()).isEqualTo(200);
		assertThat(IOUtils.toString(proxiedResponse2.getEntity().getContent())).isEqualTo("<response>Some content TWO</response>");
		assertThat(proxiedResponse2.getFirstHeader("x-http-replaying-proxy-cached")).isNull();
		
		proxiedResponse1 = httpclient.execute(httpPost1);
		assertThat(proxiedResponse1.getStatusLine().getStatusCode()).isEqualTo(200);
		assertThat(IOUtils.toString(proxiedResponse1.getEntity().getContent())).isEqualTo("<response>Some content ONE</response>");
		assertThat(proxiedResponse1.getFirstHeader("x-http-replaying-proxy-cached").getValue()).isEqualTo("true");
		
		proxiedResponse2 = httpclient.execute(httpPost2);
		assertThat(proxiedResponse2.getStatusLine().getStatusCode()).isEqualTo(200);
		assertThat(IOUtils.toString(proxiedResponse2.getEntity().getContent())).isEqualTo("<response>Some content TWO</response>");
		assertThat(proxiedResponse2.getFirstHeader("x-http-replaying-proxy-cached").getValue()).isEqualTo("true");

		server.stop();
	}

	@Test
	public void test_get_404_is_proxied_and_cached() throws Exception {
		// Given
		stubFor(get(urlEqualTo("/verify/this"))
				.withHeader("My-Header", equalTo("header-value"))
				.willReturn(aResponse()
						.withStatus(404)
				));

		HttpReplayingProxyConfiguration configuration = 
			new HttpReplayingProxyConfiguration()
		.urlToProxyTo("http://localhost:8080")
		.portToHostOn(8585);

		Server server = new HttpReplayingProxy(configuration).start();

		// When
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("http://localhost:8585/verify/this");
		httpPost.addHeader("My-Header", "header-value");
		httpPost.setEntity(new StringEntity("{\"key\":\"value\"}"));
		CloseableHttpResponse proxiedResponse = httpclient.execute(httpPost);

		// Then
		assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(404);
		assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached")).isNull();
		proxiedResponse = httpclient.execute(httpPost);
		assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(404);
		assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached").getValue()).isEqualTo("true");

		server.stop();
	}
}
