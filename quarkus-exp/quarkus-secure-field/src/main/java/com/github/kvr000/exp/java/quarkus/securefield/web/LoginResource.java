package com.github.kvr000.exp.java.quarkus.securefield.web;

import com.github.kvr000.exp.java.quarkus.securefield.security.Roles;
import com.github.kvr000.exp.java.quarkus.securefield.security.TokenService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * Login (dummy super/super) and role switching pages.
 */
@Path("/login")
@Produces(MediaType.TEXT_HTML)
public class LoginResource
{
	public static final String DUMMY_USERNAME = "super";
	public static final String DUMMY_PASSWORD = "super";

	@Inject
	Template login;

	@Inject
	Template roles;

	@Inject
	TokenService tokenService;

	@Inject
	SecurityIdentity identity;

	@Inject
	JsonWebToken jwt;

	@Context
	UriInfo uriInfo;

	@TemplateData
	public record RoleOption(String name, boolean checked)
	{
	}

	@TemplateData
	public record Claim(String name, String value)
	{
	}

	@GET
	public TemplateInstance loginPage()
	{
		return login.data("error", null)
			.data("currentUser", identity.isAnonymous() ? null : identity.getPrincipal().getName());
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response doLogin(@FormParam("username") String username, @FormParam("password") String password)
	{
		if (!DUMMY_USERNAME.equals(username) || !DUMMY_PASSWORD.equals(password)) {
			return Response.status(Response.Status.UNAUTHORIZED)
				.entity(login.data("error", "Invalid username or password").data("currentUser", null))
				.build();
		}
		String token = tokenService.issue(username, Set.of(Roles.SUPER));
		return Response.seeOther(URI.create("/login/roles"))
			.cookie(tokenService.sessionCookie(token))
			.build();
	}

	@GET
	@Path("roles")
	public Response rolesPage()
	{
		if (identity.isAnonymous()) {
			return Response.seeOther(URI.create("/login")).build();
		}
		Map<String, String> claims = new TreeMap<>();
		for (String name : jwt.getClaimNames()) {
			if (!"raw_token".equals(name)) {
				claims.put(name, String.valueOf(jwt.<Object>getClaim(name)));
			}
		}
		TemplateInstance page = roles
			.data("username", identity.getPrincipal().getName())
			.data("roles", Roles.ALL.stream().map(r -> new RoleOption(r, identity.hasRole(r))).toList())
			.data("currentRoles", identity.getRoles().isEmpty() ? "(none)" : String.join(", ", identity.getRoles()))
			.data("token", jwt.getRawToken())
			.data("expires", Instant.ofEpochSecond(jwt.getExpirationTime()).toString())
			.data("claims", claims.entrySet().stream().map(e -> new Claim(e.getKey(), e.getValue())).toList())
			.data("baseUri", uriInfo.getBaseUri().toString().replaceAll("/$", ""));
		return Response.ok(page).build();
	}

	@POST
	@Path("roles")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response changeRoles(@FormParam("roles") List<String> selected)
	{
		if (identity.isAnonymous()) {
			return Response.seeOther(URI.create("/login")).build();
		}
		Set<String> newRoles = new LinkedHashSet<>();
		if (selected != null) {
			selected.stream().filter(Roles.ALL::contains).forEach(newRoles::add);
		}
		String token = tokenService.issue(identity.getPrincipal().getName(), newRoles);
		return Response.seeOther(URI.create("/login/roles"))
			.cookie(tokenService.sessionCookie(token))
			.build();
	}
}
