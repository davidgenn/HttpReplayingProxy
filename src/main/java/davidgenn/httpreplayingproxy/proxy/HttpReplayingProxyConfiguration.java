package davidgenn.httpreplayingproxy.proxy;

import java.net.InetSocketAddress;
import java.net.URI;

public class HttpReplayingProxyConfiguration {
	
	private String proxyUrl;
	private int port = 8080;
    private String cacheRootDirectory;
	
	public HttpReplayingProxyConfiguration urlToProxyTo(String urlToProxyTo) {
		this.proxyUrl = urlToProxyTo;
		return this;
	}
	
	public HttpReplayingProxyConfiguration portToHostOn(int port) {
		this.port = port;
		return this;
	}

    public HttpReplayingProxyConfiguration withRootDirectoryForCache(String cacheRootDirectory) {
        this.cacheRootDirectory = cacheRootDirectory;
        return this;
    }

	public String getUrlToProxyTo() {
		return proxyUrl;
	}

	public int getPort() {
		return port;
	}

    public String getCacheRootDirectory() {
        return cacheRootDirectory;
    }
}
