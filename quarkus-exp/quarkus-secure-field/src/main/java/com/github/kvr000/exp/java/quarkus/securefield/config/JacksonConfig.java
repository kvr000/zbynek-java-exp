package com.github.kvr000.exp.java.quarkus.securefield.config;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import jakarta.inject.Singleton;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;


/**
 * Lenient JSON parsing for the application JsonMapper (used by Quarkus REST for request bodies):
 * trailing commas and unquoted property names, e.g. {@code {title: "x", description: "y",}}.
 */
@Singleton
public class JacksonConfig implements JsonMapperBuilderCustomizer
{
	@Override
	public void customize(JsonMapper.Builder builder)
	{
		builder
			.configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true)
			.configure(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES, true)
			.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false);
	}
}
