package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.netty;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.ResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router.RootController;
import lombok.extern.java.Log;

import java.io.IOException;

@Log
public class RoutingHandler implements HttpHandler
{
	public RoutingHandler(ContainerContext container, RootController rootController)
	{
		this.rootController = rootController;
		this.container = container;
		this.responseExchangeBuilderProvider = container.getBean(ResponseExchangeBuilderProvider.class);
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		log.info("start");
		rootController.call(new NettyRequestExchange(responseExchangeBuilderProvider, exchange), container);
		log.info("end");
	}

	private final ContainerContext container;

	private final RootController rootController;

	private final ResponseExchangeBuilderProvider responseExchangeBuilderProvider;
}
