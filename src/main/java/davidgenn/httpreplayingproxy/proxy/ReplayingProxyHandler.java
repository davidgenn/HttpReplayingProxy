package davidgenn.httpreplayingproxy.proxy;

import java.io.IOException;

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

import redis.clients.jedis.Jedis;


public class ReplayingProxyHandler  extends AbstractHandler {

	private final HttpReplayingProxyConfiguration configuration;
	private final Jedis jedis;

	public ReplayingProxyHandler(HttpReplayingProxyConfiguration configuration) {
		this.configuration = configuration;
		jedis = new Jedis("pub-redis-19186.us-east-1-3.4.ec2.garantiadata.com", 19186);
		jedis.auth("proxycache");
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {

		RequestToProxy requestToProxy = RequestToProxy.from(baseRequest);
		String cachedContent = jedis.get(requestToProxy.toString());
		if (cachedContent == null) {
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
			jedis.set(requestToProxy.toString(), content);
			jedis.expire(requestToProxy.toString(), 5);
		} else {
			response.getWriter().write(cachedContent);
		}

	}
}
