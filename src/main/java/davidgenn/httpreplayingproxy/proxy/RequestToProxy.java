package davidgenn.httpreplayingproxy.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHeader;
import org.eclipse.jetty.server.Request;

import com.google.common.collect.Lists;

public class RequestToProxy {

	private final Header[] headers;
	private final String requestPath;
	private final HttpMethod method;
	private final HttpEntity body;

	public RequestToProxy(Header[] headers, String requestPath, HttpMethod httpMethod, HttpEntity body) {
		this.headers = headers;
		this.body = body;
		this.method = httpMethod;
		this.requestPath = requestPath;
	}

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


	public Header[] getHeaders() {
		return headers;
	}


	public String getRequestPath() {
		return requestPath;
	}

	public HttpMethod getHttpMethod() {
		return method;
	}

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
}
