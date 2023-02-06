package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.benchmark.main.controller;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;


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
