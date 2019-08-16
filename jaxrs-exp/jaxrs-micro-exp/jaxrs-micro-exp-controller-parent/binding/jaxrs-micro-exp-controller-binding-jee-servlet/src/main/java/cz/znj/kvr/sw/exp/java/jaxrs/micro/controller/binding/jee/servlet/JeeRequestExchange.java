package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.jee.servlet;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.AbstractRequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.ResponseExchangeBuilderProvider;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class JeeRequestExchange extends AbstractRequestExchange
{
	public JeeRequestExchange(ResponseExchangeBuilderProvider responseExchangeBuilderProvider, HttpServletRequest request, HttpServletResponse response)
	{
		super(responseExchangeBuilderProvider);

		this.request = request;
		this.response = response;

		String queryString = request.getQueryString();

		if (queryString == null || queryString.isEmpty()) {
			this.allQueryParams = Collections.emptyMap();
		}
		else {
			String[] parameters = queryString.split("&");

			this.allQueryParams = Stream.of(parameters)
					.map(p -> p.split("=", 2))
					.map(p -> p.length == 1 ?
							new AbstractMap.SimpleImmutableEntry<>("", URLDecoder.decode(p[0], StandardCharsets.UTF_8)) :
							new AbstractMap.SimpleImmutableEntry<>(URLDecoder.decode(p[0], StandardCharsets.UTF_8), URLDecoder.decode(p[1], StandardCharsets.UTF_8)))
					.collect(Collectors.groupingBy(
							Map.Entry::getKey,
							Collectors.mapping(Map.Entry::getValue, Collectors.toList())
					));
		}

		this.allHeaders = StreamSupport.stream(Spliterators.spliteratorUnknownSize(request.getHeaderNames().asIterator(), Spliterator.ORDERED), false)
				.flatMap(name -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(request.getHeaders(name).asIterator(), Spliterator.ORDERED), false).map(value -> new AbstractMap.SimpleImmutableEntry<>(name.toLowerCase(Locale.ROOT), value)))
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
		return request.getMethod();
	}

	@Override
	public String getPath()
	{
		return request.getServletPath().substring(1);
	}

	@Override
	public InputStream getRequestBody() throws IOException
	{
		return request.getInputStream();
	}

	@Override
	public OutputStream getResponseBody() throws IOException
	{
		try {
			return response.getOutputStream();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addHeader(String name, String value)
	{
		response.addHeader(name, value);
	}

	@Override
	public void respond(int status, long contentLength)
	{
		response.setStatus(status);
	}

	private final HttpServletRequest request;

	private final HttpServletResponse response;

	@Getter
	private final Map<String, List<String>> allQueryParams;

	@Getter
	private final Map<String, List<String>> allHeaders;

	@Getter
	private final Map<String, List<Cookie>> allCookies;

}
