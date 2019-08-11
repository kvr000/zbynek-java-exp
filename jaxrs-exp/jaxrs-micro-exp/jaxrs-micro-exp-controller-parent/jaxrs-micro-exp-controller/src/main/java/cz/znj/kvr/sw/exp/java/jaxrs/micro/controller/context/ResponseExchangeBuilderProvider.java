package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context;


public interface ResponseExchangeBuilderProvider
{
	ResponseExchangeBuilder createFixedLength(RequestExchange request, int status, long length);

	ResponseExchangeBuilder createUnknownLength(RequestExchange request, int status);

	ResponseExchangeBuilder createCountingLength(RequestExchange request, int status);
}
