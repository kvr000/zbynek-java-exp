package cz.znj.kvr.sw.exp.java.guice.di.lifecycle;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.mycila.guice.ext.closeable.CloseableInjector;
import com.mycila.guice.ext.closeable.CloseableModule;
import com.mycila.guice.ext.jsr250.Jsr250Module;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;


/**
 * The test currently fails, apparently Guice does not process @PostConstruct annotation.
 */
public class LifecycleTest
{
	@Test
	public void testLifecycle()
	{
		Module module = new TestModule();
		CloseableInjector injector = Guice.createInjector(new CloseableModule(), new Jsr250Module(), module)
			.getInstance(CloseableInjector.class);
		Bean bean = injector.getInstance(Bean.class);
		AssertJUnit.assertEquals(2, bean.getState());
		injector.close();
		AssertJUnit.assertEquals(3, bean.getState());
	}

	public class TestModule extends AbstractModule
	{
		@Override
		protected void configure() {
			bind(Bean.class);
		}
	}

	@Getter
	@NoArgsConstructor()
	@Singleton
	public static class Bean
	{
		private int state = 0;

		private Injector injector;

		@Inject
		public void setInjector(Injector injector)
		{
			AssertJUnit.assertEquals(0, state);
			state = 1;
			this.injector = injector;
		}

		@PostConstruct
		public void init()
		{
			AssertJUnit.assertEquals(1, state);
			state = 2;
		}

		@PreDestroy
		public void destroy()
		{
			state = 3;
		}
	}
}
