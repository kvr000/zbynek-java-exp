package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.serialization.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


public class ObjectMapperMessageBodyWriter implements MessageBodyWriter<Object>
{
	@Inject
	public ObjectMapperMessageBodyWriter(ObjectMapper objectMapper)
	{
		this.objectMapper = objectMapper;
	}


	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		return true;
	}

	@Override
	public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
	{
		objectMapper.writeValue(entityStream, o);
	}

	private final ObjectMapper objectMapper;
}
