package com.github.kvr000.exp.java.quarkus.securefield.locale;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import net.dryuf.base.locale.LocaleContext;
import net.dryuf.base.security.identity.IdentityContext;
import net.dryuf.base.security.identity.IdentityContextHolder;

import java.util.Map;


@Path("/identity")
public class IdentityResource
{
	@Inject
	IdentityContextHolder identityContextHolder;

	@GET
	@Path("/locale")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> locale() {
		IdentityContext identityContext = identityContextHolder.getIdentityContext();
		LocaleContext localeContext = identityContext.getLocaleContext();
		return localeContext.attributes();
	}

	@GET
	@Path("/identity")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> identity() {
		IdentityContext identityContext = identityContextHolder.getIdentityContext();
		LocaleContext localeContext = identityContext.getLocaleContext();
		return ImmutableMap.of(
			"user", identityContext.getPrincipal().toString(),
			"roles", identityContext.getRoles()
		);
	}
}
