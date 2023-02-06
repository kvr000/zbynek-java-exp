package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.message;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view.Widget;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class HtmlViewResultMessageBodyWriter implements MessageBodyWriter<Widget>
{
	@Override
	public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		return Widget.class.isAssignableFrom(type) && mediaType.isCompatible(MediaType.TEXT_HTML_TYPE);
	}

	@Override
	public void writeTo(Widget view, Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
	{
		if (!view.isFinal()) {
			throw new IllegalArgumentException(getClass().getName() + " called on non-final Widget");
		}
	}
}
