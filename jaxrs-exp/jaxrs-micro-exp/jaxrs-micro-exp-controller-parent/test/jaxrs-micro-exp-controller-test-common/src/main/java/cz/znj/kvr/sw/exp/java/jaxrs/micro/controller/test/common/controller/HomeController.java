package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.common.controller;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;


/**
 * Home controller.
 */
@Path("/")
@Singleton
public class HomeController
{
	@GET
	@Path("/")
	public String homePage()
	{
		return "home";
	}
}
