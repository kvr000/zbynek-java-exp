package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.RequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.ResponseExchangeBuilder;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.CompletedServicerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.ServicerResponse;
import net.dryuf.bigio.iostream.CommittableOutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public class RootServicer
{
	@SuppressWarnings("unchecked")
	@Inject
	public RootServicer(@ServicerRouter.RootRouter ServicerRouter router, MessageBodyReader<?> bodyReader, MessageBodyWriter<?> bodyWriter)
	{
		this.router = router;
		this.bodyReader = (MessageBodyReader<Object>) bodyReader;
		this.bodyWriter = (MessageBodyWriter<Object>) bodyWriter;
	}

	public CompletableFuture<Void> call(RequestExchange requestExchange, ContainerContext container)
	{
		return router.call(requestExchange, container)
			.thenAccept((result) -> completeCall(requestExchange, container, result));
	}

	void completeCall(RequestExchange requestExchange, ContainerContext container, ServicerResponse result)
	{
		if (!result.completed()) {
			Object output = result.output();
			if (output == null) {
				respondNotFound(requestExchange);
			}
			else {
				try (ResponseExchangeBuilder response = requestExchange.startUnknownResponse(200)) {
					Optional.ofNullable(result.contentType())
						.ifPresent((contentType) ->
							response.addHeader(HttpHeaders.CONTENT_TYPE, contentType.toString())
						);
					try (CommittableOutputStream out = response.openBodyStream()) {
						try {
							bodyWriter.writeTo(result.output(), output.getClass(), output.getClass(), new Annotation[0], null, null, out);
						}
						catch (IOException ex) {
							out.committable(false);
							throw ex;
						}
					}
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void respondNotFound(RequestExchange requestExchange)
	{
		try (ResponseExchangeBuilder response = requestExchange.startUnknownResponse(Response.Status.NOT_FOUND.getStatusCode())) {
			try (CommittableOutputStream out = response.openBodyStream()) {
				out.write(Response.Status.NOT_FOUND.getReasonPhrase().getBytes(StandardCharsets.UTF_8));
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private final ServicerRouter router;

	private final MessageBodyReader<Object> bodyReader;

	private final MessageBodyWriter<Object> bodyWriter;
}
