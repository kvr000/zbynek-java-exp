package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.common.controller;

import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.RequestExchange;

import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;


/**
 * Home controller.
 */
@Path("/")
@Singleton
public class InfoController
{
	@ANYMETHOD
	@Path("/info/")
	public Map<String, Object> homePage(@Context RequestExchange request)
	{
		return ImmutableMap.of(
				"method", request.getMethod(),
				"path", request.getPath(),
				"queryParams", request.getAllQueryParams(),
				"headers", request.getAllHeaders(),
				"cookies", request.getAllCookies()
		);
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@HttpMethod("*")
	public static @interface ANYMETHOD
	{
	}
}
