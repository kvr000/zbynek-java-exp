package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;


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
