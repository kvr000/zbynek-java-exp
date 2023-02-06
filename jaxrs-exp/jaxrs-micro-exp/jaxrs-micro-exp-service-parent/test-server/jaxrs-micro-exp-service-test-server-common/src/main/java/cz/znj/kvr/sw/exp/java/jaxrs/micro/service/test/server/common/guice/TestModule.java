package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.common.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.InjectorContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.MemoryResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.ResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.ServicerRouter;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.JaxRsCaptureServicerRouter;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.RootServicer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.common.controller.HomeController;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.server.common.controller.sub.RestController;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.serialization.jackson.ObjectMapperMessageBodyReader;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.serialization.jackson.ObjectMapperMessageBodyWriter;


public class TestModule extends AbstractModule
{
	@Override
	protected void configure()
	{
		bind(HomeController.class).in(Singleton.class);
		bind(RestController.class).in(Singleton.class);
		bind(MessageBodyReader.class).to(ObjectMapperMessageBodyReader.class).in(Singleton.class);
		bind(MessageBodyWriter.class).to(ObjectMapperMessageBodyWriter.class).in(Singleton.class);
		bind(ResponseExchangeBuilderProvider.class).to(MemoryResponseExchangeBuilderProvider.class).in(Singleton.class);
		bind(ContainerContext.class).to(InjectorContainerContext.class).in(Singleton.class);
	}

	@Provides
	@Inject
	@Singleton
	//@Named("rootController")
	public RootServicer rootController(
		@ServicerRouter.RootRouter ServicerRouter rootRouter,
		MessageBodyReader reader,
		MessageBodyWriter writer
	)
	{
		return new RootServicer(rootRouter, reader, writer);
	}

	@Provides
	@Singleton
	@ServicerRouter.RootRouter
	public ServicerRouter rootRouter()
	{
		return JaxRsCaptureServicerRouter.fromClasspathJaxRsPaths(
			TestModule.class,
			"/cz/znj/kvr/sw/exp/java/jaxrs/micro/service/test/server/common/controller/JaxRsMetadata.xml"
		);
	}
}
