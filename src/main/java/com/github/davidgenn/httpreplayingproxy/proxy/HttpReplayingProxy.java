package com.github.davidgenn.httpreplayingproxy.proxy;

import org.eclipse.jetty.server.Server;

/**
 * The HttpReplayingProxy is a web app deployed in memory using Jetty.
 * <p>
 *     The configuration passed into the constructor determines what requests are proxied and where the responses should be cached.
 * </p>
 */
public class HttpReplayingProxy {
	
	private final HttpReplayingProxyConfiguration configuration;

    /**
     * Create a new HttpReplayingProxy.
     * @param configuration The configuration.
     */
	public HttpReplayingProxy(HttpReplayingProxyConfiguration configuration) {
		this.configuration = configuration;
	}

    /**
     * Start the server.
     * @return The Jetty Server.
     * @throws Exception
     */
	public Server start() throws Exception {
		Server server = new Server(configuration.getPort());
        server.setHandler(new ReplayingProxyHandler(configuration));
        server.start();
        return server;
	}
}
