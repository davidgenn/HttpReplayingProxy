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
	//private final Jedis jedis;
	private final ConcurrentHashMap<String, CachedResponse> cache = new ConcurrentHashMap<String, CachedResponse>();

	public ReplayingProxyHandler(HttpReplayingProxyConfiguration configuration) {
		this.configuration = configuration;
		//		jedis = new Jedis("pub-redis-19186.us-east-1-3.4.ec2.garantiadata.com", 19186);
		//		jedis.auth("proxycache");
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {

		RequestToProxy requestToProxy = RequestToProxy.from(baseRequest);
		LOG.info("Proxying="+requestToProxy.toString());
		CachedResponse cachedContent = cache.get(requestToProxy.toString());
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
			cache.put(requestToProxy.toString(), new CachedResponse(proxiedResponse.getStatusLine().getStatusCode(), content));
			//			jedis.expire(requestToProxy.toString(), 5);
		} else {
			LOG.info("Cache-HIT="+requestToProxy.toString());
			response.addHeader("x-http-replaying-proxy-cached", "true");
			response.getWriter().write(cachedContent.getResponse());
			response.setStatus(cachedContent.getStatusCode());
			baseRequest.setHandled(true);
		}
	}
}
