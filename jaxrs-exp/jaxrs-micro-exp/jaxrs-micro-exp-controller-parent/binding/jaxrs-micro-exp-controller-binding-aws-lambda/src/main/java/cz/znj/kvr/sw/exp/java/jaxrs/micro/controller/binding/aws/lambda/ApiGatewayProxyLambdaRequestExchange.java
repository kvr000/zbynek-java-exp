package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.AbstractRequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.ResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.util.Util;
import lombok.Getter;
import net.dryuf.concurrent.collection.LazilyBuiltLoadingCache;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.Cookie;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ApiGatewayProxyLambdaRequestExchange extends AbstractRequestExchange
{
	public ApiGatewayProxyLambdaRequestExchange(ResponseExchangeBuilderProvider responseExchangeBuilderProvider, APIGatewayV2ProxyRequestEvent requestEvent)
	{
		super(responseExchangeBuilderProvider);
		this.requestEvent = requestEvent;
		this.allQueryParams = Objects.requireNonNullElse(requestEvent.getMultiValueQueryStringParameters(), Collections.<String, List<String>>emptyMap()).entrySet().stream()
				.flatMap(entry -> entry.getValue().stream().map(value -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey().toLowerCase(Locale.ROOT), value)))
				.collect(Collectors.groupingBy(
						Map.Entry::getKey,
						Collectors.mapping(Map.Entry::getValue, Collectors.toList())
				));

		this.allHeaders = Objects.requireNonNullElse(requestEvent.getMultiValueHeaders(), Collections.<String, List<String>>emptyMap()).entrySet().stream()
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
		return requestEvent.getHttpMethod();
	}

	@Override
	public String getPath()
	{
		return requestEvent.getPath().substring(1);
	}

	@Override
	public InputStream getRequestBody()
	{
		String body = requestEvent.getBody();
		if (requestEvent.isIsBase64Encoded()) {
			return Base64.getDecoder().wrap(IOUtils.toInputStream(body));
		}
		else {
			return IOUtils.toInputStream(body);
		}
	}

	@Override
	public OutputStream getResponseBody() throws IOException
	{
		return output;
	}

	@Override
	public void addHeader(String name, String value)
	{
		if (isText == 0 && name.equalsIgnoreCase("content-type")) {
			if (TEXT_CONTENT_TYPE_PATTERN.matcher(value).matches()) {
				isText = 1;
			}
			else {
				isText = -1;
			}
		}
		else if (isText >= 0 && name.equalsIgnoreCase("content-encoding")) {
			isText = -1;
		}
		responseHeaders.computeIfAbsent(LOWERCASE_MAPPER.apply(name), key -> new ArrayList<>()).add(value);
	}

	@Override
	public void respond(int status, long contentLength)
	{
		responseStatus = status;
	}

	public APIGatewayV2ProxyResponseEvent createResponse()
	{
		APIGatewayV2ProxyResponseEvent response = new APIGatewayV2ProxyResponseEvent();
		response.setStatusCode(responseStatus);
		response.setMultiValueHeaders(responseHeaders.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toArray(Util.EMPTY_STRING_ARRAY)))
		);
		response.setIsBase64Encoded(isText <= 0);
		response.setBody(isText <= 0 ? Base64.getEncoder().encodeToString(output.toByteArray()) : new String(output.toByteArray(), StandardCharsets.UTF_8));
		return response;
	}

	private final APIGatewayV2ProxyRequestEvent requestEvent;

	@Getter
	private final Map<String, List<String>> allQueryParams;

	@Getter
	private final Map<String, List<String>> allHeaders;

	@Getter
	private final Map<String, List<Cookie>> allCookies;

	int responseStatus;

	private byte isText = 0; // convenience, -1 is binary, 0 is unknown yet, 1 is text

	private Map<String, List<String>> responseHeaders = new LinkedHashMap<>();

	private ByteArrayOutputStream output = new ByteArrayOutputStream();

	private static final Pattern TEXT_CONTENT_TYPE_PATTERN = Pattern.compile("text/[-a-zA-Z0-9_]+(|\\s*;\\s*charset=UTF-8.*)|application/json");

	private static Function<String, String> LOWERCASE_MAPPER = new LazilyBuiltLoadingCache<>(name -> name.toLowerCase(Locale.ROOT));
}
