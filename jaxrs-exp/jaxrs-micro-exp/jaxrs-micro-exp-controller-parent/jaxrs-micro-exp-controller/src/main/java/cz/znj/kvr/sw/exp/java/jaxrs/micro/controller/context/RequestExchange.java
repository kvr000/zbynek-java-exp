package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context;

import net.dryuf.bigio.iostream.CommittableOutputStream;
import org.glassfish.jersey.message.internal.CommittingOutputStream;

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

	/**
	 * Returns all query parameters, keys are lower-case.
	 *
	 * @return
	 * 	map of all query parameters, with lower-case keys
	 */
	Map<String, List<String>> getAllQueryParams();

	Cookie getCookie(String name);

	List<Cookie> getCookies(String name);

	/**
	 * Returns all cookies parameters, keys are lower-case.
	 *
	 * @return
	 * 	map of all cookies parameters, with lower-case keys
	 */
	Map<String, List<Cookie>> getAllCookies();

	String getHeader(String name);

	List<String> getHeaders(String name);

	/**
	 * Returns all headers, keys are lower-case.
	 *
	 * @return
	 * 	map of all headers, with lower-case keys
	 */
	Map<String, List<String>> getAllHeaders();

	InputStream getRequestBody() throws IOException;

	CommittableOutputStream getResponseBody() throws IOException;

	void addHeader(String name, String value);

	void addCookie(String name, String value);

	void addCookie(String name, String value, String path);

	void addCookie(String name, String value, String path, long expires);

	void respond(int status, long contentLength) throws IOException;

	ResponseExchangeBuilder startUnknownResponse(int status) throws IOException;

	ResponseExchangeBuilder startFixedResponse(int status, long length) throws IOException;

	ResponseExchangeBuilder startChunkedResponse(int status) throws IOException;
}
