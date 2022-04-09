package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.bodies;

import lombok.RequiredArgsConstructor;
import net.dryuf.concurrent.collection.LazilyBuiltLoadingCache;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;


public class MessageBodyWriterResolver
{
	private final List<MessageBodyWriter<?>> messageBodyWriters;

	private final LazilyBuiltLoadingCache<WriterKey, Optional<MessageBodyWriter<?>>> resolvedMessageBodyWriters;

	@Inject
	public MessageBodyWriterResolver(List<MessageBodyWriter<?>> messageBodyWriters)
	{
		this.messageBodyWriters = messageBodyWriters;
		this.resolvedMessageBodyWriters = new LazilyBuiltLoadingCache<>(this::resolveWriter);
	}

	public <T> Optional<MessageBodyWriter<T>> findMessageBodyWriter(Class<? extends T> clazz, Type genericType, Annotation annotations[], MediaType mediaType)
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Optional<MessageBodyWriter<T>> result = (Optional<MessageBodyWriter<T>>) (Optional)
			resolvedMessageBodyWriters.apply(new WriterKey(clazz, genericType, annotations, mediaType));
		return result;
	}

	private Optional<MessageBodyWriter<?>> resolveWriter(WriterKey key)
	{
		for (MessageBodyWriter<?> writer: messageBodyWriters) {
			if (writer.isWriteable(key.clazz, key.genericType, key.annotations, key.mediaType)) {
				return Optional.of(writer);
			}
		}
		return Optional.empty();
	}

	@RequiredArgsConstructor
	private static class WriterKey
	{
		private final Class<?> clazz;
		private final Type genericType;
		private final Annotation annotations[];
		private final MediaType mediaType;
	}
}
