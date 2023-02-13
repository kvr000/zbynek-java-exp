package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.serialization.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
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
