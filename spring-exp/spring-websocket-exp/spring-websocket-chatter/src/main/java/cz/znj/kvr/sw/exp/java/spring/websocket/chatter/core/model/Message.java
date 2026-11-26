package cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.model;

import lombok.Value;

@Value
@lombok.Builder
public class Message
{
	private String username;

	private long time;

	private String content;
}
