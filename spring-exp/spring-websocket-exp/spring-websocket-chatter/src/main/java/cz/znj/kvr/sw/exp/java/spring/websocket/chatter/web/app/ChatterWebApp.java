package cz.znj.kvr.sw.exp.java.spring.websocket.chatter.web.app;

import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.config.AppConfig;
import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.web.config.WebMvcConfig;
import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.web.config.WebSocketConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableAutoConfiguration(exclude = { ErrorMvcAutoConfiguration.class })
@Configuration
@Import({ AppConfig.class, WebMvcConfig.class, WebSocketConfig.class})
public class ChatterWebApp extends SpringApplication
{
	public static void main(String[] args)
	{
		SpringApplication.run(ChatterWebApp.class, args);
	}
}
