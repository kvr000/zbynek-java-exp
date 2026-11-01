package com.github.kvr000.exp.java.quarkus.securefield.container;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import net.dryuf.quarkus.filter.IdentityContextFilter;
import net.dryuf.quarkus.filter.RequestLogFilter;
import net.dryuf.quarkus.security.QuarkusIdentityContextHolder;
import net.dryuf.quarkus.security.QuarkusIdentityContextResolver;


/**
 * Picks individual dryuf-quarkus classes for bean discovery (CDI Lite build compatible extension), instead of
 * indexing the whole jar.  The classes are then treated as if they were in the application, i.e. their own scope
 * annotations and @Inject constructors apply.
 *
 * Registered in META-INF/services/jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension .
 */
public class DryufBeansExtension implements BuildCompatibleExtension
{
	@Discovery
	public void discover(ScannedClasses scannedClasses)
	{
		scannedClasses.add(QuarkusIdentityContextHolder.class.getName());
		scannedClasses.add(QuarkusIdentityContextResolver.class.getName());
		scannedClasses.add(RequestLogFilter.class.getName());
		scannedClasses.add(IdentityContextFilter.class.getName());
	}
}
