package cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Message
{
	private String username;

	private long time;

	private String content;
}
