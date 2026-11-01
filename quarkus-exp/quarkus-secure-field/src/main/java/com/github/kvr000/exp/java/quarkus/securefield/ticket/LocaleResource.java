package com.github.kvr000.exp.java.quarkus.securefield.ticket;

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


@Path("/static")
public class LocaleResource
{
	@Inject
	IdentityContextHolder identityContextHolder;

	@GET
	@Path("/locale")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> locale() {
		IdentityContext identityContext = identityContextHolder.getIdentityContext();
		LocaleContext localeContext = identityContext.getLocaleContext();
		return ImmutableMap.of(
			"locale", localeContext.locale().getDisplayName(),
			"timeZone", localeContext.timeZone().getId()
		);
	}
}
