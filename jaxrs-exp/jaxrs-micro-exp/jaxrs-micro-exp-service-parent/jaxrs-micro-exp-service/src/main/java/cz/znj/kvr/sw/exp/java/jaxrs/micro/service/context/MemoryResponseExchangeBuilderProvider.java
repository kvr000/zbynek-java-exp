package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context;


public class MemoryResponseExchangeBuilderProvider implements ResponseExchangeBuilderProvider
{
	@Override
	public ResponseExchangeBuilder createFixedLength(RequestExchange request, int status, long length)
	{
		return new MemoryResponseExchangeBuilder(request, status);
	}

	@Override
	public ResponseExchangeBuilder createUnknownLength(RequestExchange request, int status)
	{
		return new MemoryResponseExchangeBuilder(request, status);
	}

	@Override
	public ResponseExchangeBuilder createCountingLength(RequestExchange request, int status)
	{
		return new MemoryResponseExchangeBuilder(request, status);
	}
}
