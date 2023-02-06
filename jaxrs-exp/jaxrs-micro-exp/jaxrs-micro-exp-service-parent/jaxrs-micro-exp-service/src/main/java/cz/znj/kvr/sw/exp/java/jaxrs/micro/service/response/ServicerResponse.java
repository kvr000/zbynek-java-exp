package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response;

import jakarta.ws.rs.core.MediaType;


public interface ServicerResponse
{
	boolean direct();

	boolean completed();

	MediaType contentType();

	Object output();
}
