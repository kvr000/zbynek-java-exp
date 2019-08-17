package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import com.google.inject.Guice;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.aws.lambda.ApiGatewayProxyLambdaHandler;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.aws.lambda.ApiGatewayProxyLambdaRequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.common.guice.TestModule;
import lombok.extern.java.Log;


@Log
public class TestApiGatewayLambdaHandler extends ApiGatewayProxyLambdaHandler
{
	public TestApiGatewayLambdaHandler() throws Exception
	{
		super(createContainer());

		long started = System.currentTimeMillis();
		log.info(() -> "Started in "+(started-baseTime)+" ms");
	}

	@Override
	public APIGatewayV2ProxyResponseEvent handleRequest(APIGatewayV2ProxyRequestEvent request)
	{
		ApiGatewayProxyLambdaRequestExchange requestExchange = new ApiGatewayProxyLambdaRequestExchange(responseExchangeBuilderProvider, request);
		rootController.call(requestExchange, container);
		APIGatewayV2ProxyResponseEvent response = requestExchange.createResponse();
		log.info(response.toString());
		return response;
	}

	private static ContainerContext createContainer()
	{
		return Guice.createInjector(new TestModule()).getInstance(ContainerContext.class);
	}

	private static long baseTime = System.currentTimeMillis();
}
