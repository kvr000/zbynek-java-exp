package cz.znj.kvr.sw.exp.java.guice.di.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import cz.znj.kvr.sw.exp.java.guice.di.common.Facade;
import cz.znj.kvr.sw.exp.java.guice.di.common.First;
import cz.znj.kvr.sw.exp.java.guice.di.common.Second;
import cz.znj.kvr.sw.exp.java.guice.di.common.impl.FacadeImpl;
import cz.znj.kvr.sw.exp.java.guice.di.common.impl.FirstImpl;
import cz.znj.kvr.sw.exp.java.guice.di.common.impl.SecondImpl;
import lombok.Getter;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Inject;


/**
 * This is not supported, the module has to be installed.
 */
public class ProvideSubModuleTest
{
	@Test(expected = Exception.class)
	public void testInject()
	{
		Module module = new TestModule();
		Injector injector = Guice.createInjector(module);
		Second bean = injector.getInstance(Second.class);
		Assert.assertNotNull(bean);
	}

	public class TestModule extends AbstractModule
	{
		@Override
		protected void configure()
		{
			bind(First.class).to(FirstImpl.class);
		}

		@Provides
		@Singleton
		public ChildModule subModule() {
			return new ChildModule();
		}
	}

	public static class ChildModule extends AbstractModule
	{
		@Override
		protected void configure()
		{
			bind(Second.class).to(SecondImpl.class);
		}
	}
}
