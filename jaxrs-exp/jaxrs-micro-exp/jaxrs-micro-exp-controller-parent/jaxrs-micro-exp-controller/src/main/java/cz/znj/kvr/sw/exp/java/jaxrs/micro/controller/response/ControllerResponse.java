package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response;


import javax.ws.rs.core.MediaType;


public interface ControllerResponse
{
	boolean direct();

	boolean completed();

	MediaType contentType();

	Object output();
}
