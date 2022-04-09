package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context;

import net.dryuf.bigio.iostream.CommittableOutputStream;
import net.dryuf.bigio.iostream.FilterCommittableOutputStream;

import javax.ws.rs.core.Cookie;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Testing {@link RequestExchange}, for unit testing purposes.
 */
public class TestRequestExchange extends AbstractRequestExchange
{
	public TestRequestExchange(
			ResponseExchangeBuilderProvider responseExchangeBuilderProvider,
			String method,
			String urlPart,
			Map<String, List<String>> headers,
			InputStream requestBody
	)
	{
		super(responseExchangeBuilderProvider);
		this.method = method;
		try {
			this.url = new URL("http://localhost"+(urlPart.startsWith("/") ? "" : "/")+urlPart);
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		this.allHeaders = headers.entrySet().stream()
				.flatMap(entry -> entry.getValue().stream().map(value -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey().toLowerCase(Locale.ROOT), value)))
				.collect(Collectors.groupingBy(
						Map.Entry::getKey,
						Collectors.mapping(Map.Entry::getValue, Collectors.toList())
				));
		this.queryParameters = Stream.of(Optional.ofNullable(this.url.getQuery()).orElse("").split("&"))
				.map(p -> p.split("=", 2))
				.map(p -> p.length == 1 ?
						new AbstractMap.SimpleImmutableEntry<>("", URLDecoder.decode(p[0], StandardCharsets.UTF_8)) :
						new AbstractMap.SimpleImmutableEntry<>(URLDecoder.decode(p[0], StandardCharsets.UTF_8), URLDecoder.decode(p[1], StandardCharsets.UTF_8)))
				.collect(Collectors.groupingBy(
						Map.Entry::getKey,
						Collectors.mapping(Map.Entry::getValue, Collectors.toList())
				));
		this.requestBody = requestBody;
	}

	public TestRequestExchange(
			String method,
			String urlPart,
			Map<String, List<String>> headers,
			InputStream requestBody
	)
	{
		this(new MemoryResponseExchangeBuilderProvider(), method, urlPart, headers, requestBody);
	}

	public static TestRequestExchange fromGet(
			String urlPart,
			Map<String, List<String>> headers
	)
	{
		return new TestRequestExchange("GET", urlPart, headers, null);
	}

	@Override
	public String getMethod()
	{
		return method;
	}

	@Override
	public String getPath()
	{
		return url.getPath().substring(1);
	}

	@Override
	public Map<String, List<String>> getAllQueryParams()
	{
		return queryParameters;
	}

	public Map<String, List<Cookie>> getAllCookies()
	{
		return Collections.emptyMap();
	}

	@Override
	public Map<String, List<String>> getAllHeaders()
	{
		return allHeaders;
	}

	@Override
	public InputStream getRequestBody() throws IOException
	{
		return requestBody;
	}

	@Override
	public CommittableOutputStream getResponseBody() throws IOException
	{
		return new FilterCommittableOutputStream(responseBody)
		{
			@Override
			public void committable(boolean committable)
			{
				committed = committable;
			}
		};
	}

	@Override
	public void addHeader(String name, String value)
	{
		outputHeaders.computeIfAbsent(name, (key) -> new ArrayList<>()).add(value);
	}

	@Override
	public void respond(int status, long contentLength) throws IOException
	{
		responseStatus = status;
	}

	public int getResponseStatus()
	{
		return responseStatus;
	}

	public byte[] getResponseContent()
	{
		return responseBody.toByteArray();
	}

	public Map<String, List<String>> getOutputHeaders()
	{
		return outputHeaders;
	}

	private final String method;

	private final URL url;

	private final Map<String, List<String>> queryParameters;

	private final Map<String, List<String>> allHeaders;

	private final Map<String, List<String>> outputHeaders = new LinkedHashMap<>();

	int responseStatus;

	boolean committed;

	private final InputStream requestBody;

	private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
}
