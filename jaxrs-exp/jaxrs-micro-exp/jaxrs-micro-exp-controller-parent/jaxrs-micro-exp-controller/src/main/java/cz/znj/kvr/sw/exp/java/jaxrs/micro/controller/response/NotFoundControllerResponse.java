package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response;


public class NotFoundControllerResponse extends PartialControllerResponse
{
	public NotFoundControllerResponse()
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
