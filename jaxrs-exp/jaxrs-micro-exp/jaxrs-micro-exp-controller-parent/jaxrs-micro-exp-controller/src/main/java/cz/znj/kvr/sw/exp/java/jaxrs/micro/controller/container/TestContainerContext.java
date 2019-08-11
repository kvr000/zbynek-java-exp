package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container;

import java.util.function.Function;


public class TestContainerContext extends AbstractContainerContext
{
	public TestContainerContext(Function<Class<?>, Object> beans)
	{
		this.beans = beans;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getBean(Class<T> name)
	{
		return (T) beans.apply(name);
	}

	private Function<Class<?>, Object> beans;
}
