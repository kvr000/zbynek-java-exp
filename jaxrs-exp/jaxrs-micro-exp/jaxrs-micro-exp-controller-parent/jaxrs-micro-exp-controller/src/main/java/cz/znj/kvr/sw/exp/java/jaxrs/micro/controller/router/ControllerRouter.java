package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.RequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response.ControllerResponse;


public interface ControllerRouter
{
	ControllerResponse call(RequestExchange requestExchange, ContainerContext container);
}
