package com.github.kvr000.exp.java.springboot.basicwebapp.servlet;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Arrays;


@Log4j2
@Service
public class InitializationService
{
	@Inject
	private ServerProperties serverProperties;

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx)
	{
		return args -> {
			log.info("Available beans:");

			String[] beanNames = ctx.getBeanDefinitionNames();
			Arrays.sort(beanNames);
			for (String beanName: beanNames) {
				log.info("\t{}", beanName);
			}
		};
	}

	@EventListener
	public void onApplicationEvent(ServletWebServerInitializedEvent event)
	{
		int port = event.getWebServer().getPort();

		log.info("Listening on port: {}", port);
	}

}
