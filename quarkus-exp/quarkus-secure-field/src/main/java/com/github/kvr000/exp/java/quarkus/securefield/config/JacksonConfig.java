package com.github.kvr000.exp.java.quarkus.securefield.config;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;


/**
 * Lenient JSON parsing for the application ObjectMapper (used by Quarkus REST for request bodies):
 * trailing commas and unquoted field names, e.g. {@code {title: "x", description: "y",}}.
 */
@Singleton
public class JacksonConfig implements ObjectMapperCustomizer
{
	@Override
	public void customize(ObjectMapper objectMapper)
	{
		objectMapper.enable(
			JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(),
			JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature()
		);
	}
}
