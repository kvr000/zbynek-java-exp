package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.RequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.ResponseExchangeBuilder;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response.CompletedControllerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response.ControllerResponse;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


public class RootController
{
	@SuppressWarnings("unchecked")
	@Inject
	public RootController(@Named("rootRouter") ControllerRouter router, MessageBodyReader<?> bodyReader, MessageBodyWriter<?> bodyWriter)
	{
		this.router = router;
		this.bodyReader = (MessageBodyReader<Object>) bodyReader;
		this.bodyWriter = (MessageBodyWriter<Object>) bodyWriter;
	}

	public ControllerResponse call(RequestExchange requestExchange, ContainerContext container)
	{
		ControllerResponse result = router.call(requestExchange, container);
		if (result.completed()) {
			return result;
		}
		else {
			Object output = result.output();
			if (output == null) {
				try (ResponseExchangeBuilder response = requestExchange.startUnknownResponse(404)) {
					try (OutputStream out = response.openBodyStream()) {
						out.write("Not Found".getBytes(StandardCharsets.UTF_8));
					}
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			else {
				try (ResponseExchangeBuilder response = requestExchange.startUnknownResponse(200)) {
					Optional.ofNullable(result.contentType())
						.ifPresent((contentType) ->
							response.addHeader(HttpHeaders.CONTENT_TYPE, contentType.toString())
						);
					try (OutputStream out = response.openBodyStream()) {
						bodyWriter.writeTo(result.output(), output.getClass(), output.getClass(), new Annotation[0], null, null, out);
					}
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return new CompletedControllerResponse();
	}

	private final ControllerRouter router;

	private final MessageBodyReader<Object> bodyReader;

	private final MessageBodyWriter<Object> bodyWriter;
}
