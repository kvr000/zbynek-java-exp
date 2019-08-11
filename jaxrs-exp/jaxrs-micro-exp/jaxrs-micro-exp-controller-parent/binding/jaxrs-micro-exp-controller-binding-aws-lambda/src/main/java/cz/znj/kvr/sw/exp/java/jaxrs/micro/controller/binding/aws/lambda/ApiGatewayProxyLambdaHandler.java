package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.aws.lambda;

import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.ResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router.RootController;


public class ApiGatewayProxyLambdaHandler extends RequestHandler2
{
	public ApiGatewayProxyLambdaHandler(ContainerContext container)
	{
		this.container = container;
		this.rootController = getRootController(container);
		this.responseExchangeBuilderProvider = getResponseExchangeBuilderProvider(container);
	}

	protected RootController getRootController(ContainerContext container)
	{
		return container.getBean(RootController.class);
	}

	protected ResponseExchangeBuilderProvider getResponseExchangeBuilderProvider(ContainerContext container)
	{
		return container.getBean(ResponseExchangeBuilderProvider.class);
	}

	public APIGatewayV2ProxyResponseEvent handleRequest(APIGatewayV2ProxyRequestEvent request)
	{
		ApiGatewayProxyLambdaRequestExchange requestExchange = new ApiGatewayProxyLambdaRequestExchange(responseExchangeBuilderProvider, request);
		rootController.call(requestExchange, container);
		return requestExchange.createResponse();
	}

	private final ContainerContext container;

	private final RootController rootController;

	private final ResponseExchangeBuilderProvider responseExchangeBuilderProvider;
}
