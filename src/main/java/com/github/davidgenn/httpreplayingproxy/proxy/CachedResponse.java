package com.github.davidgenn.httpreplayingproxy.proxy;

/**
 * An element in the FileBasedCache.
 */
class CachedResponse {

    private final String content;
    private final int statusCode;
	private final RequestToProxy requestToProxy;

    /**
     * Creates a CachedResponse.
     * @param statusCode The status code.
     * @param requestToProxy The request to proxy.
     * @param content The content of the response.
     */
    public CachedResponse(int statusCode, RequestToProxy requestToProxy, String content) {
		this.requestToProxy = requestToProxy;
		this.statusCode = statusCode;
        this.content = content;
	}

    /**
     * @return The status code.
     */
	public int getStatusCode() {
		return statusCode;
	}

    /**
     * @return The request to proxy.
     */
	public RequestToProxy getRequestToProxy() {
		return requestToProxy;
	}

    /**
     * @return The cached content of the response.
     */
    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CachedResponse that = (CachedResponse) o;

        if (statusCode != that.statusCode) return false;
        if (requestToProxy != null ? !requestToProxy.equals(that.requestToProxy) : that.requestToProxy != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = statusCode;
        result = 31 * result + (requestToProxy != null ? requestToProxy.hashCode() : 0);
        return result;
    }
}
