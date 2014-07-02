package davidgenn.httpreplayingproxy.proxy;

public class CachedResponse {
	
	private int statusCode;
	private RequestToProxy response;

    public CachedResponse() {
    }

    public CachedResponse(int statusCode, RequestToProxy response) {
		this.response = response;
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public RequestToProxy getResponse() {
		return response;
	}

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setResponse(RequestToProxy response) {
        this.response = response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CachedResponse that = (CachedResponse) o;

        if (statusCode != that.statusCode) return false;
        if (response != null ? !response.equals(that.response) : that.response != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = statusCode;
        result = 31 * result + (response != null ? response.hashCode() : 0);
        return result;
    }
}
