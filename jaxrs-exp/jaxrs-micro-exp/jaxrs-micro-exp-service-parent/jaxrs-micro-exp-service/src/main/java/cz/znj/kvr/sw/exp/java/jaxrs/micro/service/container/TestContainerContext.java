package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import lombok.Getter;

import java.util.Map;


public class TestContainerContext extends AbstractContainerContext
{
	@Getter
	private final Injector injector;

	public TestContainerContext(Map<Key<?>, Object> beans)
	{
		injector = Guice.createInjector(new AbstractModule()
		{
			@SuppressWarnings("unchecked")
			@Override
			protected void configure()
			{
				super.configure();
				beans.forEach((key, instance) -> {
					bind((Key<Object>) key).toInstance(instance);
				});
			}
		});
	}
}
