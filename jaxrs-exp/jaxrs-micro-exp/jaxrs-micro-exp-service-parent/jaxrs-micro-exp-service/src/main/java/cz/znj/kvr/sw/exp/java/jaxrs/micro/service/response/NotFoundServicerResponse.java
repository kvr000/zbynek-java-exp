package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response;


public class NotFoundServicerResponse extends PartialServicerResponse
{
	public NotFoundServicerResponse()
	{
		super(null, null);
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
