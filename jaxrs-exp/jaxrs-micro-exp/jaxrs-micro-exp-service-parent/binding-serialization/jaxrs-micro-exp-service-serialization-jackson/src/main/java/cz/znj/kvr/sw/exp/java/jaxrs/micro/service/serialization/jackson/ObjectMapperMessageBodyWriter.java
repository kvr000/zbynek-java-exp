package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.serialization.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
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
