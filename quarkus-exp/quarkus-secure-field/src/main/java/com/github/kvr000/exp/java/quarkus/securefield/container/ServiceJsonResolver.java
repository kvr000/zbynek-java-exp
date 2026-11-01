package com.github.kvr000.exp.java.quarkus.securefield.container;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;


@Provider
public class ServiceJsonResolver implements ContextResolver<ObjectMapper>
{

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}
