package com.github.kvr000.exp.java.spatial.restjpa;

import com.github.kvr000.exp.java.spatial.restjpa.configuration.AppConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Optional;
import java.util.OptionalLong;


@SpringBootApplication
@EnableWebMvc
@Import({ AppConfiguration.class })
public class Application
{
	public static void main(String[] args)
	{
		System.err.printf("Starting, JarApplication took: %d ms\n", Optional.ofNullable(Long.getLong("JarApplication.start")).map(l -> System.currentTimeMillis()-l).orElse(-1L));
		SpringApplication.run(Application.class, args);
	}
}
