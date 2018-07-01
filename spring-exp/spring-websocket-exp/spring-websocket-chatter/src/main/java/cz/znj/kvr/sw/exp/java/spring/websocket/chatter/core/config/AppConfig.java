package cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.config;

import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.service.MessageService;
import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.service.impl.MemoryMessageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AppConfig
{
	@Bean
	public MessageService messageService()
	{
		return new MemoryMessageService();
	}
}
