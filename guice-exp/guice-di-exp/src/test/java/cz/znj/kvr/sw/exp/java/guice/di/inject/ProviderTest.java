package cz.znj.kvr.sw.exp.java.guice.di.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import cz.znj.kvr.sw.exp.java.guice.di.common.Facade;
import cz.znj.kvr.sw.exp.java.guice.di.common.First;
import cz.znj.kvr.sw.exp.java.guice.di.common.Second;
import cz.znj.kvr.sw.exp.java.guice.di.common.impl.FacadeImpl;
import cz.znj.kvr.sw.exp.java.guice.di.common.impl.FirstImpl;
import cz.znj.kvr.sw.exp.java.guice.di.common.impl.SecondImpl;
import lombok.Getter;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import jakarta.inject.Inject;


public class ProviderTest
{
	@Test
	public void testInject()
	{
		Module module = new TestModule();
		Injector injector = Guice.createInjector(module);
		MyBean bean = injector.getInstance(MyBean.class);
		AssertJUnit.assertEquals(2, bean.getFacade().getSecond().getSecondValue());
	}

	public class TestModule extends AbstractModule
	{
		@Override
		protected void configure() {
			bind(First.class).to(FirstImpl.class);
			bind(Second.class).to(SecondImpl.class);
		}

		@Provides
		public Facade facade(First first, Second second)
		{
			return new FacadeImpl().setFirst(first).setSecond(second).init();
		}
	}

	@Getter
	public static class MyBean
	{
		@Inject
		private Facade facade;
	}
}
