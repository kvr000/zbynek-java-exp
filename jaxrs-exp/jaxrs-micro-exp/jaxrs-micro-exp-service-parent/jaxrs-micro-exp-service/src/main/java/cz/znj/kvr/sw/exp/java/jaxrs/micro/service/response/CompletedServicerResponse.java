package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response;


import javax.ws.rs.core.MediaType;


public class CompletedServicerResponse extends AbstractServicerResponse
{
	@Override
	public boolean completed()
	{
		return true;
	}

	@Override
	public MediaType contentType()
	{
		return null;
	}

	@Override
	public Object output()
	{
		throw new IllegalStateException("Already completed");
	}
}
