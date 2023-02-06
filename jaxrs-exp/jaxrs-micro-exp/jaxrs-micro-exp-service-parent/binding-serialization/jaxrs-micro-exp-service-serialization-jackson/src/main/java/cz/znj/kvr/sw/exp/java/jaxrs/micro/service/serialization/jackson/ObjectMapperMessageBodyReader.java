package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.serialization.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


public class ObjectMapperMessageBodyReader implements MessageBodyReader<Object>
{
	@Inject
	public ObjectMapperMessageBodyReader(ObjectMapper objectMapper)
	{
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		return true;
	}

	@Override
	public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
	{
		return objectMapper.readValue(entityStream, objectMapper.constructType(genericType));
	}

	private final ObjectMapper objectMapper;
}
