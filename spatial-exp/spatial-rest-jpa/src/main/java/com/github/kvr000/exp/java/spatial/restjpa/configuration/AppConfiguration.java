package com.github.kvr000.exp.java.spatial.restjpa.configuration;

import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.configuration.SpatialDbConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@Import({ SpatialDbConfiguration.class })
public class AppConfiguration
{
}
