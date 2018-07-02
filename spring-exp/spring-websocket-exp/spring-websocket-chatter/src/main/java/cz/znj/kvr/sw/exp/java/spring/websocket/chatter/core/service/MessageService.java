package cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.service;

import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.model.Message;

import java.util.function.Consumer;

public interface MessageService
{
	void postMessage(Message message);

	void listenMessages(Consumer<Message> messageListener);
}
