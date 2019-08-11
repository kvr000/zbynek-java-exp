package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.AbstractRequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.ResponseExchangeBuilderProvider;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.Cookie;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;


public class ApiGatewayProxyLambdaRequestExchange extends AbstractRequestExchange
{
	public ApiGatewayProxyLambdaRequestExchange(ResponseExchangeBuilderProvider responseExchangeBuilderProvider, APIGatewayV2ProxyRequestEvent requestEvent)
	{
		super(responseExchangeBuilderProvider);
		this.requestEvent = requestEvent;
	}

	@Override
	public String getMethod()
	{
		return requestEvent.getHttpMethod();
	}

	@Override
	public String getPath()
	{
		return requestEvent.getPath();
	}

	@Override
	public Map<String, List<String>> getAllQueryParams()
	{
		return requestEvent.getMultiValueQueryStringParameters();
	}

	@Override
	public Map<String, List<Cookie>> getAllCookies()
	{
		return Optional.ofNullable(requestEvent.getHeaders().get("Cookie"))
				.map(Cookie::valueOf)
				.map(c -> Collections.singletonMap(c.getName(), Collections.singletonList(c)))
				.orElse(Collections.emptyMap());
	}

	@Override
	public Map<String, List<String>> getAllHeaders()
	{
		return requestEvent.getMultiValueHeaders();
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
		headers.put(name, value);
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
		response.setHeaders(headers);
		response.setIsBase64Encoded(isText <= 0);
		response.setBody(isText <= 0 ? Base64.getEncoder().encodeToString(output.toByteArray()) : new String(output.toByteArray(), StandardCharsets.UTF_8));
		return response;
	}

	private final APIGatewayV2ProxyRequestEvent requestEvent;

	int responseStatus;

	private byte isText = 0; // convenience, -1 is binary, 0 is unknown yet, 1 is text

	private Map<String, String> headers = new LinkedHashMap<>();

	private ByteArrayOutputStream output = new ByteArrayOutputStream();

	private static final Pattern TEXT_CONTENT_TYPE_PATTERN = Pattern.compile("text/[-a-zA-Z0-9_]+(|\\s*;\\s*charset=UTF-8.*)|application/json");
}
