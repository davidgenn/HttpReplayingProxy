package davidgenn.httpreplayingproxy.proxy;

import org.eclipse.jetty.server.Server;

public class HttpReplayingProxy {
	
	private final HttpReplayingProxyConfiguration configuration;
	
	public HttpReplayingProxy(HttpReplayingProxyConfiguration configuration) {
		this.configuration = configuration;
	}
	
	public Server start() throws Exception {
		Server server = new Server(configuration.getPort());
        server.setHandler(new ReplayingProxyHandler(configuration));
        server.start();
        return server;
	}
}
