package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.sun.httpserver;

import com.sun.net.httpserver.HttpExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.AbstractRequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.ResponseExchangeBuilderProvider;

import javax.ws.rs.core.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class HttpExchangeRequestExchange extends AbstractRequestExchange
{
	public HttpExchangeRequestExchange(ResponseExchangeBuilderProvider responseExchangeBuilderProvider, HttpExchange httpExchange)
	{
		super(responseExchangeBuilderProvider);

		this.httpExchange = httpExchange;

		URI requestUri = httpExchange.getRequestURI();
		String queryString = requestUri.getRawQuery();

		if (queryString == null || queryString.isEmpty()) {
			this.queryParameters = Collections.emptyMap();
		}
		else {
			String[] parameters = queryString.split("&");

			this.queryParameters = Stream.of(parameters)
					.map(p -> p.split("=", 2))
					.map(p -> p.length == 1 ?
							new AbstractMap.SimpleImmutableEntry<>("", URLDecoder.decode(p[0], StandardCharsets.UTF_8)) :
							new AbstractMap.SimpleImmutableEntry<>(URLDecoder.decode(p[0], StandardCharsets.UTF_8), URLDecoder.decode(p[1], StandardCharsets.UTF_8)))
					.collect(Collectors.groupingBy(
							Map.Entry::getKey,
							Collectors.mapping(Map.Entry::getValue, Collectors.toList())
					));
		}
	}

	@Override
	public String getMethod()
	{
		return httpExchange.getRequestMethod();
	}

	@Override
	public String getPath()
	{
		return httpExchange.getRequestURI().getPath().substring(1);
	}

	@Override
	public Map<String, List<String>> getAllQueryParams()
	{
		return queryParameters;
	}

	@Override
	public Map<String, List<Cookie>> getAllCookies()
	{
		return Collections.emptyMap();
	}

	@Override
	public Map<String, List<String>> getAllHeaders()
	{
		return httpExchange.getRequestHeaders().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public InputStream getRequestBody() throws IOException
	{
		return httpExchange.getRequestBody();
	}

	@Override
	public OutputStream getResponseBody() throws IOException
	{
		return httpExchange.getResponseBody();
	}

	@Override
	public void respond(int status, long length) throws IOException
	{
		try {
			httpExchange.sendResponseHeaders(status, length);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addHeader(String name, String value)
	{
		httpExchange.getResponseHeaders().add(name, value);
	}

	private final HttpExchange httpExchange;

	private final Map<String, List<String>> queryParameters;
}
