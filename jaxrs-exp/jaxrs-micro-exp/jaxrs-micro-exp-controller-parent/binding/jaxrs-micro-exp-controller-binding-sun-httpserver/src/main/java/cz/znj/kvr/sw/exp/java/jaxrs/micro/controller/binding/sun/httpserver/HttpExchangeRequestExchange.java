package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.sun.httpserver;

import com.sun.net.httpserver.HttpExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.AbstractRequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.ResponseExchangeBuilderProvider;
import lombok.Getter;

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
import java.util.Locale;
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
			this.allQueryParams = Collections.emptyMap();
		}
		else {
			String[] parameters = queryString.split("&");

			this.allQueryParams = Stream.of(parameters)
					.map(p -> p.split("=", 2))
					.map(p -> p.length == 1 ?
							new AbstractMap.SimpleImmutableEntry<>("", URLDecoder.decode(p[0], StandardCharsets.UTF_8)) :
							new AbstractMap.SimpleImmutableEntry<>(URLDecoder.decode(p[0], StandardCharsets.UTF_8).toLowerCase(Locale.ROOT), URLDecoder.decode(p[1], StandardCharsets.UTF_8)))
					.collect(Collectors.groupingBy(
							Map.Entry::getKey,
							Collectors.mapping(Map.Entry::getValue, Collectors.toList())
					));
		}

		this.allHeaders = httpExchange.getRequestHeaders().entrySet().stream()
				.flatMap(entry -> entry.getValue().stream().map(value -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey().toLowerCase(Locale.ROOT), value)))
				.collect(Collectors.groupingBy(
						Map.Entry::getKey,
						Collectors.mapping(Map.Entry::getValue, Collectors.toList())
				));

		this.allCookies = this.allHeaders.getOrDefault("cookie", Collections.emptyList()).stream()
				.map(Cookie::valueOf)
				.collect(Collectors.groupingBy(Cookie::getName));

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

	@Getter
	private final Map<String, List<String>> allQueryParams;

	@Getter
	private final Map<String, List<String>> allHeaders;

	@Getter
	private final Map<String, List<Cookie>> allCookies;
}
