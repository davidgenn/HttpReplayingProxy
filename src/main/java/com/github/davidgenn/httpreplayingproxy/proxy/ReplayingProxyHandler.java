package com.github.davidgenn.httpreplayingproxy.proxy;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.http.HttpClient;

/**
 * The core Jetty handler that receives the requests that are to be proxied.
 * <p>Firstly it determines if the request is already present in the cache. If not, it calls the 'real' service.</p>
 */
class ReplayingProxyHandler  extends AbstractHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ReplayingProxyHandler.class);

    private final HttpReplayingProxyConfiguration configuration;
    private final ConcurrentHashMap<String, CachedResponse> cache = new ConcurrentHashMap<String, CachedResponse>();
    private final FileBasedCache fileBasedCache;

    /**
     * Returns a ReplayingProxyHandler.
     * @param configuration The configuration.
     * @throws IOException
     */
    public ReplayingProxyHandler(HttpReplayingProxyConfiguration configuration) throws IOException {
        this.configuration = configuration;
        fileBasedCache = new FileBasedCache(configuration.getCacheRootDirectory(), configuration.getTimeToLiveSeconds());
    }

    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        if (request.getRequestURI().equals("/favicon.ico")) {return;}

        RequestToProxy requestToProxy = RequestToProxy.from(baseRequest, configuration.getMatchHeaders());
        LOG.info("Proxying="+requestToProxy.toString());
        CachedResponse cachedContent = fileBasedCache.get(requestToProxy);
        if (cachedContent == null) {
            LOG.info("Cache-MISS="+requestToProxy.toString());

            CloseableHttpResponse proxiedResponse = callRealService(requestToProxy);

            String content = IOUtils.toString(proxiedResponse.getEntity().getContent());
            response.setStatus(proxiedResponse.getStatusLine().getStatusCode());
            baseRequest.setHandled(true);
            Header contentTypeHeader = proxiedResponse.getFirstHeader("Content-Type");
            if (contentTypeHeader != null) {
                response.addHeader("Content-Type", contentTypeHeader.getValue());
            }
            response.getWriter().write(content);
            fileBasedCache.put(requestToProxy.getRequestPath(), new CachedResponse(proxiedResponse.getStatusLine().getStatusCode(), requestToProxy, content, contentTypeHeader == null ? "" : contentTypeHeader.getValue()));
        } else {
            LOG.info("Cache-HIT=" + requestToProxy.toString());
            response.addHeader("x-http-replaying-proxy-cached", "true");
            response.getWriter().write(cachedContent.getContent());
            response.setStatus(cachedContent.getStatusCode());
            response.addHeader("Content-Type", cachedContent.getContentType());
            baseRequest.setHandled(true);
        }
    }

    private CloseableHttpResponse callRealService(RequestToProxy requestToProxy) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpRequestBase httpRequest = null;

        switch (requestToProxy.getHttpMethod()) {
            case GET:
                httpRequest = new HttpGet(configuration.getUrlToProxyTo() + requestToProxy.getRequestPath());
                break;
            case POST:
                httpRequest = new HttpPost(configuration.getUrlToProxyTo() + requestToProxy.getRequestPath());
                ((HttpPost)httpRequest).setEntity(requestToProxy.getBody());
                break;
            case PUT:
                httpRequest = new HttpPut(configuration.getUrlToProxyTo() + requestToProxy.getRequestPath());
                ((HttpPut)httpRequest).setEntity(requestToProxy.getBody());
                break;
            case DELETE:
                httpRequest = new HttpDelete(configuration.getUrlToProxyTo() + requestToProxy.getRequestPath());
                break;
            case OPTIONS:
                httpRequest = new HttpOptions(configuration.getUrlToProxyTo() + requestToProxy.getRequestPath());
                break;
            default:
                throw new RuntimeException("Http Method="+ requestToProxy.getHttpMethod() + " is currently unsupported. Please raise a ticket.");
        }

        httpRequest.setHeaders(requestToProxy.getHeaders());

        return httpclient.execute(httpRequest);
    }
}
