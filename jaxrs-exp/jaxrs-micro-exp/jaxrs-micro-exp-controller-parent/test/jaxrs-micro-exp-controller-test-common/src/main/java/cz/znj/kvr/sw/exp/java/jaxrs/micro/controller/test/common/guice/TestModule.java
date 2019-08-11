package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.common.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.guice.GuiceContainer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.MemoryResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.ResponseExchangeBuilderProvider;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router.ControllerRouter;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router.JaxRsCaptureControllerRouter;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router.RootController;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.common.controller.HomeController;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.common.controller.sub.RestController;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.jackson.ObjectMapperMessageBodyReader;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.jackson.ObjectMapperMessageBodyWriter;

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
		bind(ContainerContext.class).to(GuiceContainer.class).in(Singleton.class);
	}

	@Provides
	@Inject
	@Singleton
	//@Named("rootController")
	public RootController rootController(@Named("rootRouter") ControllerRouter rootRouter, MessageBodyReader reader, MessageBodyWriter writer)
	{
		return new RootController(rootRouter, reader, writer);
	}

	@Provides
	@Singleton
	@Named("rootRouter")
	public ControllerRouter rootRouter()
	{
		return JaxRsCaptureControllerRouter.fromClasspathJaxRsPaths(TestModule.class, "/cz/znj/kvr/sw/exp/java/jaxrs/micro/controller/test/common/controller/JaxRsMetadata.xml");
	}
}
