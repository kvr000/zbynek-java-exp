package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.test.sub;

import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Data;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.Map;


/**
 * Home controller.
 */
@Path("/sub")
public class RestController
{
	@GET
	@Path("/")
	public Map<String, Object> page()
	{
		return ImmutableMap.of("name", "Zbynek");
	}

	@POST
	@Path("/")
	public boolean pagePost()
	{
		return true;
	}

	@POST
	@Path("/deeper/{user}/")
	public DeeperData deeper(@PathParam("user") String user)
	{
		return DeeperData.builder()
				.name("Zbynek Vyskovsky")
				.age(33)
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
