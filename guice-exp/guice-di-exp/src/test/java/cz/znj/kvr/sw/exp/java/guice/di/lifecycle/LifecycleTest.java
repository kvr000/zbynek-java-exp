package cz.znj.kvr.sw.exp.java.guice.di.lifecycle;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.mycila.guice.ext.closeable.CloseableModule;
import com.mycila.guice.ext.jsr250.Jsr250Module;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.inject.Inject;


/**
 * The test currently fails, apparently Guice does not process @PostConstruct annotation.
 */
public class LifecycleTest
{
	@Test
	public void testLifecycle()
	{
		Module module = new TestModule();
		Injector injector = Guice.createInjector(module, new Jsr250Module(), new CloseableModule());
		Bean bean = injector.getInstance(Bean.class);
		Assert.assertEquals(2, bean.getState());
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
	public static class Bean
	{
		private int state = 0;

		private Injector injector;

		@Inject
		public void setInjector(Injector injector)
		{
			Assert.assertEquals(0, state);
			state = 1;
			this.injector = injector;
		}

		@PostConstruct
		public void init()
		{
			Assert.assertEquals(1, state);
			state = 2;
		}
	}
}
