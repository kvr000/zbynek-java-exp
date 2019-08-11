package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.jee.servlet;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.ResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router.RootController;
import lombok.extern.java.Log;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Log
public class RoutingServlet extends HttpServlet
{
	public RoutingServlet(ContainerContext container, RootController rootController)
	{
		this.rootController = rootController;
		this.container = container;
		this.responseExchangeBuilderProvider = container.getBean(ResponseExchangeBuilderProvider.class);
	}

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		log.info("start");
		rootController.call(new JeeRequestExchange(responseExchangeBuilderProvider, request, response), container);
		log.info("end");
	}

	private final ContainerContext container;

	private final RootController rootController;

	private final ResponseExchangeBuilderProvider responseExchangeBuilderProvider;
}
