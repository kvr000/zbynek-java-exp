package cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.service.impl;

import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.model.Message;
import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.service.MessageService;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


public class MemoryMessageService implements MessageService
{
	private Set<Consumer<Message>> listeners = Collections.newSetFromMap(new ConcurrentHashMap<Consumer<Message>, Boolean>());

	@Override
	public void postMessage(Message message)
	{
		for (Iterator<Consumer<Message>> listenersIterator = listeners.iterator(); listenersIterator.hasNext(); ) {
			Consumer<Message> listener = listenersIterator.next();
			try {
				listener.accept(message);
			}
			catch (Exception ex) {
				listenersIterator.remove();
			}
		}
	}

	@Override
	public void listenMessages(Consumer<Message> messageListener)
	{
		listeners.add(messageListener);
	}
}
