package cz.znj.kvr.sw.exp.java.guice.di.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import cz.znj.kvr.sw.exp.java.guice.di.common.First;
import cz.znj.kvr.sw.exp.java.guice.di.common.Second;
import cz.znj.kvr.sw.exp.java.guice.di.common.impl.FirstImpl;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.inject.Inject;


public class MultiModuleTest
{
	@Test
	public void testInject()
	{
		Injector injector = Guice.createInjector(new FirstTestModule(), new SecondTestModule());
		Second bean = injector.getInstance(Second.class);
		AssertJUnit.assertEquals(1, bean.getSecondValue());
	}

	public class FirstTestModule extends AbstractModule
	{
		@Override
		protected void configure() {
			bind(First.class).to(FirstImpl.class);
		}
	}


	public class SecondTestModule extends AbstractModule
	{
		@Override
		protected void configure() {
			bind(Second.class).to(MyBean.class);
		}
	}

	public static class MyBean implements Second {
		private First first;

		@Inject
		public void setFirst(First first) {
			this.first = first;
		}

		public int getSecondValue() {
			return first.getFirstValue();
		}
	}
}
