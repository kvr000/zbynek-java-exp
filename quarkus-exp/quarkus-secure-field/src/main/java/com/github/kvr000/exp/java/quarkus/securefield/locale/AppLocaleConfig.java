package com.github.kvr000.exp.java.quarkus.securefield.locale;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import net.dryuf.quarkus.locale.LocaleConfig;

import java.time.ZoneId;


/**
 * Locale configuration, app.locale.* in application.properties .
 */
@ConfigMapping(prefix = "app.locale")
public interface AppLocaleConfig extends LocaleConfig
{
	@Override
	@WithDefault("UTC")
	ZoneId defaultTimeZone();
}
