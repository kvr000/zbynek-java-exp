package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.BeanMethod;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reflect.OwnedMethodHolder;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reflect.MethodArgumentsResolver;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
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

	BeanMethod resolveMethod(OwnedMethodHolder method, BiFunction<ContainerContext, OwnedMethodHolder, MethodArgumentsResolver> invokerProvider);
}
