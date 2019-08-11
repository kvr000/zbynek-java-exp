package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context;

import java.io.IOException;


public abstract class AbstractResponseExchangeBuilder implements ResponseExchangeBuilder
{
	protected AbstractResponseExchangeBuilder(RequestExchange request, int status)
	{
		this.request = request;
		this.status = status;
	}

	@Override
	public void addHeader(String name, String value)
	{
		request.addHeader(name, value);
	}

	@Override
	public void addCookie(String name, String value)
	{
		request.addCookie(name, value);
	}

	@Override
	public void addCookie(String name, String value, String path)
	{
		request.addCookie(name, value, path);
	}

	@Override
	public void addCookie(String name, String value, String path, long expires)
	{
		request.addCookie(name, value, path, expires);
	}

	@Override
	public void close() throws IOException
	{
		request.respond(status, getOutputLength());
	}

	protected abstract long getOutputLength();

	protected final RequestExchange request;

	int status;
}
