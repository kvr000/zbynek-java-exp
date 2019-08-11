package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.guice;

import com.google.inject.Injector;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.AbstractContainerContext;

import javax.inject.Inject;


public class GuiceContainer extends AbstractContainerContext
{
	@Inject
	public GuiceContainer(Injector injector)
	{
		this.injector = injector;
	}

	@Override
	public <T> T getBean(Class<T> clazz)
	{
		return injector.getInstance(clazz);
	}

	private final Injector injector;
}
