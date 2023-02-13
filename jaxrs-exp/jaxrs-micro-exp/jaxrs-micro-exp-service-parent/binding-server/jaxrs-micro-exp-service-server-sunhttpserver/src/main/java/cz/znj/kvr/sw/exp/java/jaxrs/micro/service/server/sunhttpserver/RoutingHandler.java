package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.server.sunhttpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.ResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.RootServicer;
import lombok.extern.java.Log;

import java.io.IOException;

@Log
public class RoutingHandler implements HttpHandler
{
	public RoutingHandler(ContainerContext container, RootServicer rootServicer)
	{
		this.rootServicer = rootServicer;
		this.container = container;
		this.responseExchangeBuilderProvider = container.getInjector().getInstance(ResponseExchangeBuilderProvider.class);
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		log.info("start");
		rootServicer.call(new HttpExchangeRequestExchange(responseExchangeBuilderProvider, exchange), container).join();
		log.info("end");
	}

	private final ContainerContext container;

	private final RootServicer rootServicer;

	private final ResponseExchangeBuilderProvider responseExchangeBuilderProvider;
}
