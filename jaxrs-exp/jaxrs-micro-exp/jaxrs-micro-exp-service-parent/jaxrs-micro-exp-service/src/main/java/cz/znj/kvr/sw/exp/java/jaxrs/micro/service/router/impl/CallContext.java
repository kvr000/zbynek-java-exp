package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.impl;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.RequestContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.RequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.path.PathResolver;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.lang.reflect.Type;
import java.util.List;

public class CallContext
{
	RequestExchange serverRequest() { return requestExchange; }

	public ContainerContext container()
	{
		return requestContext.container();
	}

	public MediaType contentType()
	{
		return requestContext.headers().getMediaType();
	}

	public List<MediaType> acceptsType()
	{
		return requestContext.headers().getAcceptableMediaTypes();
	}

	public SecurityContext securityContext()
	{
		return requestContext.securityContext();
	}

	public Request request()
	{
		return requestContext.request();
	}

	public Response response()
	{
		return requestContext.response();
	}

	public Object customContext()
	{
		return requestContext.customContext();
	}

	public Object readBody(Type type)
	{
		return requestContext.input();
	}

	public RequestExchange requestExchange;

	public RequestContext requestContext;

	public PathResolver.Match<FunctionData> match;
}
