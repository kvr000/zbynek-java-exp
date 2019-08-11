package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;


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
