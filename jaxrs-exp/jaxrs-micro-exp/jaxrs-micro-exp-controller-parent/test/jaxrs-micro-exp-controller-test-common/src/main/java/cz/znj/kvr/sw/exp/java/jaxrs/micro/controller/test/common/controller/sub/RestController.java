package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.common.controller.sub;

import lombok.Builder;
import lombok.Data;

import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.Map;


/**
 * Home controller.
 */
@Path("/sub")
@Singleton
public class RestController
{
	@GET
	@Path("/")
	public Map<String, Object> page()
	{
		return Map.of("name", "Zbynek");
	}

	@POST
	@Path("/")
	public boolean pagePost()
	{
		return true;
	}

	@GET
	@Path("/deeper/{user}/")
	public DeeperData deeper(@PathParam("user") String user, @DefaultValue("-1") @QueryParam("age") int age)
	{
		return DeeperData.builder()
				.name(user)
				.age(age)
				.build();
	}

	@POST
	@Path("/deeper/{user}/")
	public boolean deeperPost(@PathParam("user") String user, DeeperData data)
	{
		return true;
	}

	@Data
	@Builder
	public static class DeeperData
	{
		private String name;

		private int age;
	}
}
