package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.BeanMethod;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.OwnedMethodHolder;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.MethodInvokerStatic;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.function.BiFunction;


public interface RequestContext
{
	RequestExchange serverRequest();

	ContainerContext container();

	SecurityContext securityContext();

	Request request();

	Response response();

	HttpHeaders headers();

	UriInfo uriInfo();

	Object customContext();

	InputStream input();

	BeanMethod resolveMethod(OwnedMethodHolder method, BiFunction<ContainerContext, OwnedMethodHolder, MethodInvokerStatic> invokerProvider);
}
