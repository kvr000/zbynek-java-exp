package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;


/**
 * Home controller.
 */
@Path("/")
public class HomeController
{
	@GET
	@Path("/")
	public String homePage()
	{
		return "home";
	}
}
