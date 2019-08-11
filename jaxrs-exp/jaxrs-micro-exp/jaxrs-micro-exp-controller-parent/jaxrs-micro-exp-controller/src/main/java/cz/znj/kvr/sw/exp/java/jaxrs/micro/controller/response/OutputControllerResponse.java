package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.ws.rs.core.MediaType;


public class OutputControllerResponse extends AbstractControllerResponse
{
	public OutputControllerResponse(MediaType contentType, Object output)
	{
		this.contentType = contentType;
		this.output = output;
	}

	@Getter
	@Accessors(fluent = true)
	private final MediaType contentType;

	@Getter
	@Accessors(fluent = true)
	private final Object output;
}
