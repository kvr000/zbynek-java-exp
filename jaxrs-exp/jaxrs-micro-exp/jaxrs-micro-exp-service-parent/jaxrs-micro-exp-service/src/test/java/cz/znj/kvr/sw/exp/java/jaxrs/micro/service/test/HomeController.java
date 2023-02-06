package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;


/**
 *
 */
@Path("/")
public class HomeController
{
	@GET
	@Path("/")
	@Produces("text/plain")
	public String homePage()
	{
		return "Hello";
	}
}
