package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response;


public class UnsupportedRequestServicerResponse extends PartialServicerResponse
{
	public UnsupportedRequestServicerResponse()
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
