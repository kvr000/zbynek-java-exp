package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


public interface RequestExchange
{
	String getMethod();

	String getPath();

	MediaType getContentType();

	List<MediaType> getAcceptsType();

	String getQueryParam(String name);

	List<String> getQueryParams(String name);

	Map<String, List<String>> getAllQueryParams();

	Cookie getCookie(String name);

	List<Cookie> getCookies(String name);

	Map<String, List<Cookie>> getAllCookies();

	String getHeader(String name);

	List<String> getHeaders(String name);

	Map<String, List<String>> getAllHeaders();

	InputStream getRequestBody() throws IOException;

	OutputStream getResponseBody() throws IOException;

	void addHeader(String name, String value);

	void addCookie(String name, String value);

	void addCookie(String name, String value, String path);

	void addCookie(String name, String value, String path, long expires);

	void respond(int status, long contentLength) throws IOException;

	ResponseExchangeBuilder startUnknownResponse(int status) throws IOException;

	ResponseExchangeBuilder startFixedResponse(int status, long length) throws IOException;

	ResponseExchangeBuilder startChunkedResponse(int status) throws IOException;
}
