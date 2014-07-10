package com.github.davidgenn.httpreplayingproxy.proxy;

/**
 * Configures the behaviour of a HttpReplayingProxy.
 */
public class HttpReplayingProxyConfiguration {
	
	private String proxyUrl;
	private int port = 8080;
    private String cacheRootDirectory;
    private long timeToLiveSeconds = Long.MAX_VALUE / 2000; // will be multiplied by 1000 to turn into milliseconds.
    private MatchHeaders matchHeaders = MatchHeaders.MATCH_NAME_ONLY;

    /**
     * @param urlToProxyTo The 'real' url to proxy calls to.
     * @return The HttpReplayingProxyConfiguration.
     */
	public HttpReplayingProxyConfiguration urlToProxyTo(String urlToProxyTo) {
		this.proxyUrl = urlToProxyTo;
		return this;
	}

    /**
     * @param port The port the HttpReplayingProxy should listen on.
     * @return The HttpReplayingProxyConfiguration.
     */
	public HttpReplayingProxyConfiguration portToHostOn(int port) {
		this.port = port;
		return this;
	}

    /**
     * @param cacheRootDirectory The directory the cached responses should be stored in.
     * @return The HttpReplayingProxyConfiguration.
     */
    public HttpReplayingProxyConfiguration withRootDirectoryForCache(String cacheRootDirectory) {
        this.cacheRootDirectory = cacheRootDirectory;
        return this;
    }

    /**
     * @param timeToLiveSeconds The time to live for the cache in seconds.
     * @return The HttpReplayingProxyConfiguration.
     */
    public HttpReplayingProxyConfiguration timeToLiveForCacheInSeconds(long timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
        return this;
    }

    /**
     * @param matchHeaders How headers should be treated when looking for a previously cached response.
     * @return The HttpReplayingProxyConfiguration.
     */
    public HttpReplayingProxyConfiguration treatHeaders(MatchHeaders matchHeaders) {
        this.matchHeaders = matchHeaders;
        return this;
    }

    /**
     * @return The url being proxied.
     */
	public String getUrlToProxyTo() {
		return proxyUrl;
	}

    /**
     * @return The port the HttpReplayingProxy is listening on.
     */
	public int getPort() {
		return port;
	}

    /**
     * @return The directory the cached responses should be stored in.
     */
    public String getCacheRootDirectory() {
        return cacheRootDirectory;
    }

    /**
     * @return The time to live for the cache in seconds.
     */
    public long getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    /**
     * @return How headers should be treated when looking for a previously cached response.
     */
    public MatchHeaders getMatchHeaders() {
        return matchHeaders;
    }
}
