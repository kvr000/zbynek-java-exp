package com.github.kvr000.exp.java.quarkus.securefield.container;

import com.github.kvr000.exp.java.quarkus.securefield.locale.AppLocaleConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import net.dryuf.quarkus.locale.RequestLocaleProvider;


/**
 * Beans from dryuf-quarkus which need application specific configuration.  Others are simply listed in
 * {@link DryufBeansExtension}.
 */
@ApplicationScoped
public class IdentityContextBeans
{
	/**
	 * RequestLocaleProvider is a plain class, configured by application's own config mapping.
	 * @Singleton, not @ApplicationScoped: the class has no no-arg constructor, so it cannot be client-proxied.
	 */
	@Produces
	@Singleton
	public RequestLocaleProvider requestLocaleProvider(AppLocaleConfig localeConfig)
	{
		return new RequestLocaleProvider(localeConfig);
	}
}
