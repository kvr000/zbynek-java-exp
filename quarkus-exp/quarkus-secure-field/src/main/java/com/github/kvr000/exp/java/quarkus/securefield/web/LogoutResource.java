package com.github.kvr000.exp.java.quarkus.securefield.web;

import com.github.kvr000.exp.java.quarkus.securefield.security.TokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;


/**
 * Clears the session cookie.
 */
@Path("/logout")
public class LogoutResource
{
	@Inject
	TokenService tokenService;

	@GET
	public Response logout()
	{
		return Response.seeOther(URI.create("/"))
			.cookie(tokenService.clearCookie())
			.build();
	}
}
