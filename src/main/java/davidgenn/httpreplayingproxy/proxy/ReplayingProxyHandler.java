package davidgenn.httpreplayingproxy.proxy;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ReplayingProxyHandler  extends AbstractHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ReplayingProxyHandler.class);

	private final HttpReplayingProxyConfiguration configuration;
	private final ConcurrentHashMap<String, CachedResponse> cache = new ConcurrentHashMap<String, CachedResponse>();
    private final FileBasedCache fileBasedCache;

	public ReplayingProxyHandler(HttpReplayingProxyConfiguration configuration) throws IOException {
		this.configuration = configuration;
        fileBasedCache = new FileBasedCache("C:/Users/David/HttpReplayingProxy/src/test/java/davidgenn/httpreplayingproxy/cache/");
    }

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {

		RequestToProxy requestToProxy = RequestToProxy.from(baseRequest);
		LOG.info("Proxying="+requestToProxy.toString());
		CachedResponse cachedContent = fileBasedCache.get(requestToProxy);
		if (cachedContent == null) {
			LOG.info("Cache-MISS="+requestToProxy.toString());
			CloseableHttpClient httpclient = HttpClients.createDefault();

			HttpRequestBase httpRequest = null;

			switch (requestToProxy.getHttpMethod()) {
			case GET:
				httpRequest = new HttpGet(configuration.getUrlToProxyTo() + requestToProxy.getRequestPath());
				break;
			case POST:
				httpRequest = new HttpPost(configuration.getUrlToProxyTo() + requestToProxy.getRequestPath());
				((HttpPost)httpRequest).setEntity(requestToProxy.getBody());
			default:
				break;
			}

			httpRequest.setHeaders(requestToProxy.getHeaders());

			CloseableHttpResponse proxiedResponse = httpclient.execute(httpRequest);
			String content = IOUtils.toString(proxiedResponse.getEntity().getContent());
			response.setStatus(proxiedResponse.getStatusLine().getStatusCode());
			baseRequest.setHandled(true);
			response.getWriter().write(content);
            fileBasedCache.put(requestToProxy.getRequestPath(), new CachedResponse(proxiedResponse.getStatusLine().getStatusCode(), requestToProxy, content));
		} else {
			LOG.info("Cache-HIT="+requestToProxy.toString());
			response.addHeader("x-http-replaying-proxy-cached", "true");
			response.getWriter().write(cachedContent.getContent());
			response.setStatus(cachedContent.getStatusCode());
			baseRequest.setHandled(true);
		}
	}
}
