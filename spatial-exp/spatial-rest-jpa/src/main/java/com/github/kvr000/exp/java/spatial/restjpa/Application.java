package com.github.kvr000.exp.java.spatial.restjpa;

import com.github.kvr000.exp.java.spatial.restjpa.configuration.AppConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@SpringBootApplication
@EnableWebMvc
@Import({ AppConfiguration.class })
public class Application
{
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
