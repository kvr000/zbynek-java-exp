package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response;

import lombok.Getter;
import lombok.experimental.Accessors;

import jakarta.ws.rs.core.MediaType;


public class OutputServicerResponse extends AbstractServicerResponse
{
	public OutputServicerResponse(MediaType contentType, Object output)
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
