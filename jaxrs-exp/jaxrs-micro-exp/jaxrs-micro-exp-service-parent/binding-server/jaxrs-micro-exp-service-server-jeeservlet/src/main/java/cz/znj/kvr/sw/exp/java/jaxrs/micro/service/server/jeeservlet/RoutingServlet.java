package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.server.jeeservlet;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.ResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.RootServicer;
import lombok.extern.java.Log;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Log
public class RoutingServlet extends HttpServlet
{
	public RoutingServlet(ContainerContext container, RootServicer rootServicer)
	{
		this.rootServicer = rootServicer;
		this.container = container;
		this.responseExchangeBuilderProvider = container.getInjector().getInstance(ResponseExchangeBuilderProvider.class);
	}

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		log.info("start");
		rootServicer.call(new JeeRequestExchange(responseExchangeBuilderProvider, request, response), container).join();
		log.info("end");
	}

	private final ContainerContext container;

	private final RootServicer rootServicer;

	private final ResponseExchangeBuilderProvider responseExchangeBuilderProvider;
}
