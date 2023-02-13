package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.common.controller;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


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
