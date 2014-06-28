package davidgenn.httpreplayingproxy.proxy;

import java.net.InetSocketAddress;
import java.net.URI;

public class HttpReplayingProxyConfiguration {
	
	private String proxyUrl;
	private int port = 8080;
	
	public HttpReplayingProxyConfiguration urlToProxyTo(String urlToProxyTo) {
		this.proxyUrl = urlToProxyTo;
		return this;
	}
	
	public HttpReplayingProxyConfiguration portToHostOn(int port) {
		this.port = port;
		return this;
	}

	public String getUrlToProxyTo() {
		return proxyUrl;
	}

	public int getPort() {
		return port;
	}

}
