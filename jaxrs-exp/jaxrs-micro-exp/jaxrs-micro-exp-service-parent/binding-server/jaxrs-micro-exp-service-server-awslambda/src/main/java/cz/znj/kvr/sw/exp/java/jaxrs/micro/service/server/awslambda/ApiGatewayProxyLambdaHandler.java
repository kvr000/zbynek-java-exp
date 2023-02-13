package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.server.awslambda;

import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.ResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.RootServicer;


public class ApiGatewayProxyLambdaHandler extends RequestHandler2
{
	public ApiGatewayProxyLambdaHandler(ContainerContext container)
	{
		this.container = container;
		this.rootServicer = getRootController(container);
		this.responseExchangeBuilderProvider = getResponseExchangeBuilderProvider(container);
	}

	protected RootServicer getRootController(ContainerContext container)
	{
		return container.getInjector().getInstance(RootServicer.class);
	}

	protected ResponseExchangeBuilderProvider getResponseExchangeBuilderProvider(ContainerContext container)
	{
		return container.getInjector().getInstance(ResponseExchangeBuilderProvider.class);
	}

	public APIGatewayV2ProxyResponseEvent handleRequest(APIGatewayV2ProxyRequestEvent request)
	{
		ApiGatewayProxyLambdaRequestExchange requestExchange = new ApiGatewayProxyLambdaRequestExchange(responseExchangeBuilderProvider, request);
		rootServicer.call(requestExchange, container).join();
		return requestExchange.createResponse();
	}

	protected final ContainerContext container;

	protected final RootServicer rootServicer;

	protected final ResponseExchangeBuilderProvider responseExchangeBuilderProvider;
}
