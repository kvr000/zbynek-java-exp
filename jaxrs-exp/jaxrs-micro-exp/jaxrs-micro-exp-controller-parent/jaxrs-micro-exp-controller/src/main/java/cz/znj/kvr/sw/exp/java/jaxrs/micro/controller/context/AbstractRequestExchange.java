package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context;

import net.dryuf.concurrent.collection.LazilyBuiltLoadingCache;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;


public abstract class AbstractRequestExchange implements RequestExchange
{
	public AbstractRequestExchange(ResponseExchangeBuilderProvider responseExchangeBuilderProvider)
	{
		this.responseExchangeBuilderProvider = responseExchangeBuilderProvider;
	}
	@Override
	public MediaType getContentType()
	{
		return MediaType.valueOf(getHeader(HttpHeaders.CONTENT_TYPE));
	}

	@Override
	public List<MediaType> getAcceptsType()
	{
		return Optional.ofNullable(getHeader(HttpHeaders.ACCEPT)).map(MediaType::valueOf).map(Collections::singletonList).orElse(Collections.emptyList());
	}

	@Override
	public String getQueryParam(String name)
	{
		return getQueryParams(name).stream().findFirst().orElse(null);
	}

	@Override
	public List<String> getQueryParams(String name)
	{
		return Optional.ofNullable(getAllQueryParams().get(LOWERCASE_MAPPER.apply(name))).orElse(Collections.emptyList());
	}

	@Override
	public String getHeader(String name)
	{
		List<String> headers = getHeaders(name);
		return headers.isEmpty() ? null : headers.get(0);
	}

	@Override
	public List<String> getHeaders(String name)
	{
		return getAllHeaders().getOrDefault(LOWERCASE_MAPPER.apply(name), Collections.emptyList());
	}

	@Override
	public Cookie getCookie(String name)
	{
		return getCookies(name).stream().findFirst().orElse(null);
	}

	@Override
	public List<Cookie> getCookies(String name)
	{
		return Optional.ofNullable(getAllCookies().get(LOWERCASE_MAPPER.apply(name))).orElse(Collections.emptyList());
	}

	@Override
	public void addCookie(String name, String value)
	{
		addCookie(name, value, "/");
	}

	@Override
	public void addCookie(String name, String value, String path)
	{
		addCookie(name, value, "/", -1);
	}

	@Override
	public void addCookie(String name, String value, String path, long expires)
	{
		StringBuilder cookieSpec = new StringBuilder(URLEncoder.encode(name, StandardCharsets.UTF_8))
				.append("=")
				.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
		if (path != null) {
			cookieSpec.append("; Path=").append(path.replaceAll(";", "%3b").replaceAll("\n", "%0a"));
		}
		if (expires >= 0) {
			cookieSpec.append("; Expires=").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(expires)));
		}
		addHeader("Set-Cookie", cookieSpec.toString());
	}

	@Override
	public ResponseExchangeBuilder startUnknownResponse(int status) throws IOException
	{
		return responseExchangeBuilderProvider.createCountingLength(this, status);
	}

	@Override
	public ResponseExchangeBuilder startFixedResponse(int status, long length) throws IOException
	{
		return responseExchangeBuilderProvider.createFixedLength(this, status, length);
	}

	@Override
	public ResponseExchangeBuilder startChunkedResponse(int status) throws IOException
	{
		return responseExchangeBuilderProvider.createUnknownLength(this, status);
	}

	protected final ResponseExchangeBuilderProvider responseExchangeBuilderProvider;

	private static Function<String, String> LOWERCASE_MAPPER = new LazilyBuiltLoadingCache<>(name -> name.toLowerCase(Locale.ROOT));
}
