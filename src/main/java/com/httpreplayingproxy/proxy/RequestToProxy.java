package com.httpreplayingproxy.proxy;

import java.io.IOException;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.eclipse.jetty.server.Request;

import com.google.common.collect.Lists;

/**
 * The request being proxied.
 */
class RequestToProxy {

	private Header[] headers;
	private String requestPath;
	private HttpMethod method;
	private HttpEntity body;

    /**
     * Create a new RequestToProxy.
     * @param headers The headers on the request.
     * @param requestPath The request path.
     * @param httpMethod The HTTP method.
     * @param body The request body - for POSTs and PUTs.
     */
	public RequestToProxy(Header[] headers, String requestPath, HttpMethod httpMethod, HttpEntity body) {
		this.headers = headers;
		this.body = body;
		this.method = httpMethod;
		this.requestPath = requestPath;
	}

    /**
     * Builds a RequestToProxy from an HTTP request.
     * @param baseRequest The request.
     * @return The built RequestToProxy.
     * @throws IOException
     */
	public static RequestToProxy from(Request baseRequest) throws IOException {
		Set<Header> headers = new HashSet<Header>();
		Enumeration<String> headerNames = baseRequest.getHeaderNames();
		while(headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			if ("Content-Length".equals(headerName)) {
				continue;
			}

			BasicHeader header = new BasicHeader(headerName, baseRequest.getHeader(headerName));
			headers.add(header);
		}

		ByteArrayEntity body = new ByteArrayEntity(IOUtils.toByteArray(baseRequest.getInputStream())); 
		baseRequest.extractParameters();
		String queryString = baseRequest.getQueryString();
		String path = null;
		if (queryString == null) {
			path = baseRequest.getPathInfo();	
		} else {
			path = baseRequest.getPathInfo() + "?"+ queryString;	
		}
		return new RequestToProxy(
				headers.toArray(new Header[0]), 
				path, 
				HttpMethod.valueOf(baseRequest.getMethod()), 
				body);
	}

    /**
     * @return The headers.
     */
	public Header[] getHeaders() {
		return headers;
	}

    /**
     * @return The request path.
     */
	public String getRequestPath() {
		return requestPath;
	}

    /**
     * @return The HTTP method.
     */
	public HttpMethod getHttpMethod() {
		return method;
	}

    /**
     * @return The body of the request.
     */
	public HttpEntity getBody() {
		return body;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("requestPath=" + requestPath);
		sb.append("method=" + method);
		sb.append("headers=" + headersToString());
		try {
			sb.append("body=" + new String(IOUtils.toByteArray(body.getContent()))); 
		} catch (IllegalStateException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} 
		return sb.toString();
	}
	
	private String headersToString() {
		StringBuffer sb = new StringBuffer();
		List<Header> headerList = Lists.newArrayList(headers);
		Collections.sort(headerList, new Comparator<Header>() {

			@Override
			public int compare(Header o1, Header o2) {
				return o1.getName().compareTo(o2.getName());
			}

		});
		for (Header header: headerList) {
			sb.append(header.getName() + "=" + header.getValue());
		}
		return sb.toString();
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestToProxy that = (RequestToProxy) o;

        if (body != null ? !body.equals(that.body) : that.body != null) return false;
        if (!Arrays.equals(headers, that.headers)) return false;
        if (method != that.method) return false;
        if (requestPath != null ? !requestPath.equals(that.requestPath) : that.requestPath != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = headers != null ? Arrays.hashCode(headers) : 0;
        result = 31 * result + (requestPath != null ? requestPath.hashCode() : 0);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        return result;
    }
}