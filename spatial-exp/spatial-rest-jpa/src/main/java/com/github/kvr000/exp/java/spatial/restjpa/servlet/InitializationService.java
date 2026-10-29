package com.github.kvr000.exp.java.spatial.restjpa.servlet;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.server.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;


@Log4j2
@Service
public class InitializationService
{
	@EventListener
	public void onApplicationEvent(ServletWebServerInitializedEvent event)
	{
		int port = event.getWebServer().getPort();

		log.info("Listening on port: {}", port);
	}
}
