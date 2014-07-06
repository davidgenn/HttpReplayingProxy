package com.httpreplayingproxy.proxy;

public class CachedResponse {

    private final String content;
    private final int statusCode;
	private final RequestToProxy requestToProxy;

    public CachedResponse(int statusCode, RequestToProxy requestToProxy, String content) {
		this.requestToProxy = requestToProxy;
		this.statusCode = statusCode;
        this.content = content;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public RequestToProxy getRequestToProxy() {
		return requestToProxy;
	}

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
