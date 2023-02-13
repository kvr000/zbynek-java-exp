package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.server.netty;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.AbstractRequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.ResponseExchangeBuilderProvider;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.Getter;
import net.dryuf.bigio.iostream.CommittableOutputStream;

import javax.ws.rs.core.Cookie;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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


public class NettyRequestExchange extends AbstractRequestExchange
{
	private final URI requestUri;

	private final ChannelHandlerContext handlerContext;

	private final ByteBuf requestBody;

	private ByteArrayInputStream requestBodyStream;

	public NettyRequestExchange(ResponseExchangeBuilderProvider responseExchangeBuilderProvider, HttpRequest request, ByteBuf body, ChannelHandlerContext handlerContext)
	{
		super(responseExchangeBuilderProvider);

		this.request = request;
		this.handlerContext = handlerContext;

		this.requestBody = body;
		this.requestBodyStream = new ByteArrayInputStream(body.array(), body.readerIndex(), body.writerIndex());

		requestUri = URI.create(this.request.uri());
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

		this.allHeaders = this.request.headers().entries().stream()
				.map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue()))
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
		return request.method().name();
	}

	@Override
	public String getPath()
	{
		return requestUri.getRawPath();
	}

	@Override
	public Map<String, List<String>> getAllHeaders()
	{
		return allHeaders;
	}

	@Override
	public InputStream getRequestBody() throws IOException
	{
		return requestBodyStream;
	}

	@Override
	public CommittableOutputStream getResponseBody() throws IOException
	{
		return new CommittableOutputStream()
		{
			@Override
			public void write(int b) throws IOException
			{
				handlerContext.write(new DefaultHttpContent(Unpooled.wrappedBuffer(new byte[]{ (byte) b })));
			}

			@Override
			public void write(byte[] buf, int o, int l) throws IOException
			{
				handlerContext.write(new DefaultHttpContent(Unpooled.wrappedBuffer(buf, o, l)));
			}

			@Override
			public void committable(boolean committable)
			{
				committed = committable;
			}

			@Override
			public void flush()
			{
				handlerContext.flush();
			}

			@Override
			public void close()
			{
				if (committed) {
					handlerContext.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
				}
			}
		};
	}

	@Override
	public void respond(int status, long length) throws IOException
	{
		handlerContext.writeAndFlush(new DefaultHttpResponse(
			HttpVersion.HTTP_1_1,
			HttpResponseStatus.valueOf(status),
			responseHeaders
		));
	}

	@Override
	public void addHeader(String name, String value)
	{
		responseHeaders.add(name, value);
	}

	private final HttpRequest request;

	@Getter
	private final Map<String, List<String>> allQueryParams;

	@Getter
	private final Map<String, List<String>> allHeaders;

	@Getter
	private final Map<String, List<Cookie>> allCookies;

	private final HttpHeaders responseHeaders = new DefaultHttpHeaders();

	private boolean committed;
}
