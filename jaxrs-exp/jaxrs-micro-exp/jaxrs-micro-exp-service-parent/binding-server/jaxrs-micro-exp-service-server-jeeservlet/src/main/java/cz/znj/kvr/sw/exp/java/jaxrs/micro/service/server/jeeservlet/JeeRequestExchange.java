package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.server.jeeservlet;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.AbstractRequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.ResponseExchangeBuilderProvider;
import lombok.Getter;
import net.dryuf.bigio.iostream.CommittableOutputStream;
import net.dryuf.bigio.iostream.FilterCommittableOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
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
	public CommittableOutputStream getResponseBody() throws IOException
	{
		try {
			return new FilterCommittableOutputStream(response.getOutputStream())
			{
				@Override
				public void committable(boolean committable)
				{
					committed = committable;
				}
			};
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

	private boolean committed;

}