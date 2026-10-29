package com.github.kvr000.exp.java.quarkus.securefield.web;

import com.github.kvr000.exp.java.quarkus.securefield.security.TokenService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import java.net.URI;


/**
 * Clears the session cookie.
 */
@Path("/logout")
@RequiredArgsConstructor
public class LogoutResource
{
	private final TokenService tokenService;

	@GET
	public Response logout()
	{
		return Response.seeOther(URI.create("/"))
			.cookie(tokenService.clearCookie())
			.build();
	}
}
