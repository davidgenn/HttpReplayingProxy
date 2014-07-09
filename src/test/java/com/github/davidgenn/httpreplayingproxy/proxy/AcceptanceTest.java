package com.github.davidgenn.httpreplayingproxy.proxy;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Server;
import org.junit.*;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.fest.assertions.Assertions.*;

/**
 * The acceptance tests for the HttpReplayingProxy.
 */
public class AcceptanceTest {

    public static final String ROOT_DIRECTORY = "cache/";
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);
    private Server server;

    @Before
    public void reset() throws IOException {
        String directory = rootDirectory();
        FileBasedCache.reset(directory);
    }

    private String rootDirectory() {
        if (System.getProperty("cache.root.directory") == null) {
            throw new RuntimeException("cache.root.directory System Property not set. Please set it and try again!");
        }
        System.out.println("Cache root directory: " + System.getProperty("cache.root.directory"));
        return System.getProperty("cache.root.directory");
    }

    @Test
    public void test_get_is_proxied_with_headers_and_cached() throws Exception {
        // Given
        stubFor(get(urlEqualTo("/verify/this?query=value"))
                .withHeader("My-Header", equalTo("header-value"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<response>Some content</response>")));

        startHttpReplayingProxyServer();

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
    }

    @Test
    public void test_post_is_proxied_with_headers_and_cached() throws Exception {
        // Given
        stubFor(post(urlEqualTo("/verify/thispost"))
                .withHeader("My-Header", equalTo("header-value"))
                .withRequestBody(equalTo("{\"key\":\"value\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<response>Some content</response>")));

        startHttpReplayingProxyServer();

        // When
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://localhost:8585/verify/thispost");
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

        startHttpReplayingProxyServer();

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
    }

    private void startHttpReplayingProxyServer() throws Exception {
        HttpReplayingProxyConfiguration configuration =
                new HttpReplayingProxyConfiguration()
                        .urlToProxyTo("http://localhost:8080")
                        .portToHostOn(8585)
                        .withRootDirectoryForCache(rootDirectory());

        server = new HttpReplayingProxy(configuration).start();
    }

    private void startHttpReplayingProxyServer(long ttl) throws Exception {
        HttpReplayingProxyConfiguration configuration =
                new HttpReplayingProxyConfiguration()
                        .urlToProxyTo("http://localhost:8080")
                        .portToHostOn(8585)
                        .withRootDirectoryForCache(rootDirectory())
                        .timeToLiveForCacheInSeconds(ttl);

        server = new HttpReplayingProxy(configuration).start();
    }

    @Test
    public void test_get_404_is_proxied_and_cached() throws Exception {
        // Given
        stubFor(get(urlEqualTo("/verify/this404"))
                .withHeader("My-Header", equalTo("header-value"))
                .willReturn(aResponse()
                                .withStatus(404)
                ));

        startHttpReplayingProxyServer();

        // When
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://localhost:8585/verify/this404");
        httpPost.addHeader("My-Header", "header-value");
        httpPost.setEntity(new StringEntity("{\"key\":\"value\"}"));
        CloseableHttpResponse proxiedResponse = httpclient.execute(httpPost);

        // Then
        assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(404);
        assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached")).isNull();
        proxiedResponse = httpclient.execute(httpPost);
        assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(404);
        assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached").getValue()).isEqualTo("true");
    }

    @Test
    public void test_cache_reset() throws Exception {
        // Given
        stubFor(get(urlEqualTo("/verify/this?query=value"))
                .withHeader("My-Header", equalTo("header-value"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<response>Some content</response>")));

        startHttpReplayingProxyServer();

        // When
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://localhost:8585/verify/this?query=value");
        httpGet.addHeader("My-Header", "header-value");
        CloseableHttpResponse proxiedResponse = httpclient.execute(httpGet);

        // Then

        // First call
        assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
        assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
        assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached")).isNull();

        // Second call - start the server again - this should clear the cache at startup
        server.stop();
        System.setProperty(FileBasedCache.RESET_CACHE_AT_STARTUP, "true");
        startHttpReplayingProxyServer();
        proxiedResponse = httpclient.execute(httpGet);
        assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
        assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
        assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached")).isNull(); // Still an uncached response
    }

    @Test
    public void test_cache_expiry() throws Exception {
        // Given
        stubFor(get(urlEqualTo("/verify/this?query=value"))
                .withHeader("My-Header", equalTo("header-value"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<response>Some content</response>")));

        startHttpReplayingProxyServer(1);

        // When
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://localhost:8585/verify/this?query=value");
        httpGet.addHeader("My-Header", "header-value");
        CloseableHttpResponse proxiedResponse = httpclient.execute(httpGet);

        // Then

        // First call
        assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
        assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
        assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached")).isNull();

        // Second call - should be cached
        proxiedResponse = httpclient.execute(httpGet);
        assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
        assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
        assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached").getValue()).isEqualTo("true");

        // Third call - should be refreshed
        Thread.sleep(1010); // Slightly more than a second
        proxiedResponse = httpclient.execute(httpGet);
        assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
        assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
        assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached")).isNull();
    }

    @Test
    public void test_put_is_proxied_with_headers_and_cached() throws Exception {
        // Given
        stubFor(put(urlEqualTo("/verify/thisput"))
                .withHeader("My-Header", equalTo("header-value"))
                .withRequestBody(equalTo("{\"key\":\"value\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<response>Some content</response>")));

        startHttpReplayingProxyServer();

        // When
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut("http://localhost:8585/verify/thisput");
        httpPut.addHeader("My-Header", "header-value");
        httpPut.setEntity(new StringEntity("{\"key\":\"value\"}"));
        CloseableHttpResponse proxiedResponse = httpclient.execute(httpPut);

        // Then
        assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
        assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
        assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached")).isNull();
        proxiedResponse = httpclient.execute(httpPut);
        assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
        assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
        assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached").getValue()).isEqualTo("true");
    }

    @Test
    public void test_delete_is_proxied_with_headers_and_cached() throws Exception {
        // Given
        stubFor(delete(urlEqualTo("/verify/this?query=value"))
                .withHeader("My-Header", equalTo("header-value"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<response>Some content</response>")));

        startHttpReplayingProxyServer();

        // When
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete("http://localhost:8585/verify/this?query=value");
        httpDelete.addHeader("My-Header", "header-value");
        CloseableHttpResponse proxiedResponse = httpclient.execute(httpDelete);

        // Then
        assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
        assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
        assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached")).isNull();
        proxiedResponse = httpclient.execute(httpDelete);
        assertThat(proxiedResponse.getStatusLine().getStatusCode()).isEqualTo(200);
        assertThat(IOUtils.toString(proxiedResponse.getEntity().getContent())).isEqualTo("<response>Some content</response>");
        assertThat(proxiedResponse.getFirstHeader("x-http-replaying-proxy-cached").getValue()).isEqualTo("true");
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }
}
