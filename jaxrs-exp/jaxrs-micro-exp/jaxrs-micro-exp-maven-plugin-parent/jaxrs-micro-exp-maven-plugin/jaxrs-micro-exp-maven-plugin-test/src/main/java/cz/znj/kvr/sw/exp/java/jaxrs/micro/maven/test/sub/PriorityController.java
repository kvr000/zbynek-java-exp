package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.test.sub;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Map;


/**
 * Priority test controller.
 */
@Path("/prio")
public class PriorityController
{
	@POST
	@Path("/")
	public Map<String, Object> page()
	{
		return Collections.singletonMap("name", "Zbynek");
	}


	@POST
	@Path("/{placeholder}/")
	public String placeholder()
	{
		return null;
	}


	@POST
	@Path("/query/")
	public String query()
	{
		return null;
	}

	@POST
	@QueryParam("version=1")
	@Path("/query/")
	public String queryVersion1()
	{
		return null;
	}

	@POST
	@QueryParam("version=2")
	@Path("/query/")
	public String queryVersion2()
	{
		return null;
	}


	@POST
	@Consumes("*")
	@Path("/consumes/")
	public String consumes()
	{
		return null;
	}

	@POST
	@Consumes("text/html")
	@Path("/consumes/")
	public String consumesHtml()
	{
		return null;
	}

	@POST
	@Consumes("application/json")
	@Path("/consumes/")
	public String consumesJson()
	{
		return null;
	}


	@POST
	@Produces("*")
	@Path("/produces/")
	public String produces()
	{
		return null;
	}

	@POST
	@Produces("text/html")
	@Path("/produces/")
	public String producesHtml()
	{
		return null;
	}

	@POST
	@Produces("application/json")
	@Path("/produces/")
	public String producesJson()
	{
		return null;
	}

	@ANY_EMPTY
	@Path("/method/")
	public String methodAnyEmpty()
	{
		return null;
	}

	@ANY_STAR
	@Path("/method/")
	public String methodAnyStar()
	{
		return null;
	}

	@GET
	@Path("/method/")
	public String methodGet()
	{
		return null;
	}

	@POST
	@Path("/method/")
	public String methodPost()
	{
		return null;
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@HttpMethod("")
	public static @interface ANY_EMPTY
	{
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@HttpMethod("*")
	public static @interface ANY_STAR
	{
	}
}
