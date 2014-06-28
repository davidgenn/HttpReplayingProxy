package davidgenn.httpreplayingproxy.proxy;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHeader;
import org.eclipse.jetty.server.Request;

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
		while(baseRequest.getHeaderNames().hasMoreElements()) {
			String headerName = baseRequest.getHeaderNames().nextElement();
			BasicHeader header = new BasicHeader(headerName, baseRequest.getHeader(headerName));
			headers.add(header);
		}
		
		InputStreamEntity body = new InputStreamEntity(baseRequest.getInputStream()); 
		
		return new RequestToProxy(
				(Header[]) headers.toArray(), 
				baseRequest.getPathInfo(), 
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
}
