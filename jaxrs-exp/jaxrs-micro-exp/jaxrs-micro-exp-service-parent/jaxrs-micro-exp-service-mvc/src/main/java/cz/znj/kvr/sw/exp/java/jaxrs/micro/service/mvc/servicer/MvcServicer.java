package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.servicer;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.RequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.processor.MvcRenderer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view.ModelView;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view.Widget;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.OutputServicerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.ServicerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.ServicerRouter;
import lombok.RequiredArgsConstructor;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class MvcServicer implements ServicerRouter
{
	private final MvcRenderer renderer;

	private final ServicerRouter delegate;

	@Override
	public CompletableFuture<ServicerResponse> call(RequestExchange requestExchange, ContainerContext container)
	{
		CompletableFuture<ServicerResponse> response = delegate.call(requestExchange, container);
		return response.thenApply((ServicerResponse r) -> {
			if (r.output() instanceof ModelView) {
				Widget widget = renderer.render(Response.ok().build(), (ModelView) r.output());
				return new OutputServicerResponse(
					MediaType.valueOf((String) widget.metadata().getOrDefault("contentType", MediaType.TEXT_HTML)),
					widget.content()
				);
			}
			else {
				return r;
			}
		});
	}
}
