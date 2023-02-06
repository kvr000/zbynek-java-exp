package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.benchmark.main.controller;

import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.RequestExchange;

import jakarta.inject.Singleton;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
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
