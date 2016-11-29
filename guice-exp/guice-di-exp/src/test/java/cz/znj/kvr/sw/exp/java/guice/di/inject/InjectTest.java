package cz.znj.kvr.sw.exp.java.guice.di.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Inject;


public class InjectTest
{
	@Test
	public void testInject()
	{
		Module module = new TestModule();
		Injector injector = Guice.createInjector(module);
		Bean bean = injector.getInstance(Bean.class);
		Assert.assertNotNull(bean.getInjector());
	}

	public class TestModule extends AbstractModule
	{
		@Override
		protected void configure() {
			bind(Bean.class);
		}
	}

	@Getter
	@AllArgsConstructor(onConstructor = @__(@Inject))
	public static class Bean
	{
		@Inject
		private Injector injector;
	}
}
