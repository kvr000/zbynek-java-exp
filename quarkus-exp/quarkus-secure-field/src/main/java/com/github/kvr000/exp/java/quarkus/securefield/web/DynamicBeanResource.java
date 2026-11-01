package com.github.kvr000.exp.java.quarkus.securefield.web;

import com.github.kvr000.exp.java.quarkus.securefield.bean.DynamicBean;
import io.quarkus.arc.Arc;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;


/**
 * Clears the session cookie.
 */
@Path("/bean/dynamic")
@RequiredArgsConstructor
public class DynamicBeanResource
{
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response dynamicBean()
	{
		DynamicBean dynamicBean = Arc.container().instance(DynamicBean.class).get();
		return Response.ok().entity(dynamicBean.greet()).build();
	}
}
