package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.aws.lambda;

import com.google.inject.Guice;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.aws.lambda.ApiGatewayProxyLambdaHandler;
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

	private static ContainerContext createContainer()
	{
		return Guice.createInjector(new TestModule()).getInstance(ContainerContext.class);
	}

	private static long baseTime = System.currentTimeMillis();
}
