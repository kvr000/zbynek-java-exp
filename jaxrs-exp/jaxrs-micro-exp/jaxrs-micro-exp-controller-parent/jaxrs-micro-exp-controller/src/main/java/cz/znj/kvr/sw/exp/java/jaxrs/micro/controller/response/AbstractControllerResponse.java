package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response;


public abstract class AbstractControllerResponse implements ControllerResponse
{
	@Override
	public boolean direct()
	{
		return true;
	}

	@Override
	public boolean completed()
	{
		return false;
	}
}
