package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.binding.dagger;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.AbstractContainerContext;


public class DaggerContainer extends AbstractContainerContext
{
	public DaggerContainer(Object component)
	{
		this.component = component;
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <T> T getBean(Class<T> clazz)
	{
		throw new UnsupportedOperationException("TODO");
	}

	private final Object component;
}
