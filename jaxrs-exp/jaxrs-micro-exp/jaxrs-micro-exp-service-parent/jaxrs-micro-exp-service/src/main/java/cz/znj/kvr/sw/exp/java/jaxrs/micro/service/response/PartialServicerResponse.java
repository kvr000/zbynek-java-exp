package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response;

import javax.ws.rs.core.MediaType;


public class PartialServicerResponse extends OutputServicerResponse
{
	public PartialServicerResponse(MediaType contentType, Object output)
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
