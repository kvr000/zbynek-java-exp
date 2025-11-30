package cz.znj.kvr.sw.exp.java.guice.di.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import jakarta.inject.Inject;


public class ConstructorInjectTest
{
	@Test
	public void testInject()
	{
		Module module = new TestModule();
		Injector injector = Guice.createInjector(module);
		Bean bean = injector.getInstance(Bean.class);
		AssertJUnit.assertNotNull(bean.getInjector());
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
