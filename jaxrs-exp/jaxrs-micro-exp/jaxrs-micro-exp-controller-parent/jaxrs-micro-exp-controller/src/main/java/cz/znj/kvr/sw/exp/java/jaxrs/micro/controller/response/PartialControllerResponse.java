package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response;

import javax.ws.rs.core.MediaType;


public class PartialControllerResponse extends OutputControllerResponse
{
	public PartialControllerResponse(MediaType contentType, Object output)
	{
		super(contentType, output);
	}

	@Override
	public boolean direct()
	{
		return false;
	}

	@Override
	public boolean completed()
	{
		return false;
	}
}
