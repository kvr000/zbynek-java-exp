package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.common.controller.sub;

import lombok.Builder;
import lombok.Data;

import jakarta.inject.Singleton;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
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
