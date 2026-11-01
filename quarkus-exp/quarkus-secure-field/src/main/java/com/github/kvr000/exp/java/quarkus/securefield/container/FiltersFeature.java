package com.github.kvr000.exp.java.quarkus.securefield.container;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import net.dryuf.quarkus.filter.IdentityContextFilter;
import net.dryuf.quarkus.filter.RequestLogFilter;


/**
 * Populates the request scoped IdentityContextHolder.  Replaces net.dryuf.quarkus.filter.IdentityContextFilter, which
 * is not discovered as its jar is not indexed.
 */
@Provider
public class FiltersFeature implements DynamicFeature
{
	@Inject
	IdentityContextFilter identityContextFilter;
	@Inject
	RequestLogFilter requestLogFilter;

	@Override
	public void configure(ResourceInfo resourceInfo, FeatureContext context)
	{
		context.register(requestLogFilter);
		context.register(identityContextFilter);
	}
}
