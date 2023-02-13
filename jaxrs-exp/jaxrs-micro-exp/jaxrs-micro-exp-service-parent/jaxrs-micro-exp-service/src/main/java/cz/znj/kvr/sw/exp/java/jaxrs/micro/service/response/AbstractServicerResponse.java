package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response;


public abstract class AbstractServicerResponse implements ServicerResponse
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
