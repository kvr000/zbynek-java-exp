package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.common.controller;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


/**
 * Home controller.
 */
@Path("/")
@Singleton
public class HomeController
{
	@GET
	@Path("/")
	@Produces(MediaType.TEXT_HTML)
	public String homePage()
	{
		return "home";
	}
}
