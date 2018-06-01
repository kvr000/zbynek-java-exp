package cz.znj.kvr.sw.exp.java.guice.di.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import cz.znj.kvr.sw.exp.java.guice.di.common.First;
import cz.znj.kvr.sw.exp.java.guice.di.common.Second;
import cz.znj.kvr.sw.exp.java.guice.di.common.impl.FirstImpl;
import cz.znj.kvr.sw.exp.java.guice.di.common.impl.SecondImpl;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;


public class SingletonTest
{
	@Test
	public void testSingleton()
	{
		Module module = new TestModule();
		Injector injector = Guice.createInjector(module);

		First first0 = injector.getInstance(First.class);
		First first1 = injector.getInstance(First.class);
		AssertJUnit.assertTrue("Non-singleton instances must be the same", first0 != first1);

		Second second0 = injector.getInstance(Second.class);
		Second second1 = injector.getInstance(Second.class);
		AssertJUnit.assertTrue("Singleton instances must be the same", second0 == second1);
	}

	public class TestModule extends AbstractModule
	{
		@Override
		protected void configure() {
			bind(First.class).to(FirstImpl.class);
			bind(Second.class).to(SecondImpl.class).in(Singleton.class);
		}
	}
}
