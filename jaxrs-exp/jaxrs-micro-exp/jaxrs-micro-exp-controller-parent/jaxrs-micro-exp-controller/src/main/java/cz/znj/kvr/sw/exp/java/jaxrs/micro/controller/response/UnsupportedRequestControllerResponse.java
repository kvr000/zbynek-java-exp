package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response;


public class UnsupportedRequestControllerResponse extends PartialControllerResponse
{
	public UnsupportedRequestControllerResponse()
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
