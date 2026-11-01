package com.github.kvr000.exp.java.quarkus.securefield.container;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.ext.Provider;
import lombok.RequiredArgsConstructor;
import net.dryuf.quarkus.filter.IdentityContextFilter;
import net.dryuf.quarkus.security.QuarkusIdentityContextHolder;
import net.dryuf.quarkus.security.QuarkusIdentityContextResolver;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;


/**
 * Populates the request scoped IdentityContextHolder.  Replaces net.dryuf.quarkus.filter.IdentityContextFilter, which
 * is not discovered as its jar is not indexed.
 */
@Provider
public class DryufIdentityContextFilter extends IdentityContextFilter
{
	@Inject
	public DryufIdentityContextFilter(QuarkusIdentityContextHolder identityContextHolder, QuarkusIdentityContextResolver identityContextResolver)
	{
		super(identityContextHolder, identityContextResolver);
	}

	@ServerRequestFilter
	public void populateIdentityContext(ContainerRequestContext requestContext)
	{
		super.filter(requestContext);
	}
}
