package cz.znj.kvr.sw.exp.java.guice.di.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import cz.znj.kvr.sw.exp.java.guice.di.common.First;
import cz.znj.kvr.sw.exp.java.guice.di.common.Second;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;


// this class runs in sequence so we have reliable tracking of number of singleton instances being created
@Test(singleThreaded = true)
public class MultiInterfaceSingleImplTest
{
	@Test
	public void testSingletonImpl()
	{
		Impl.instancesCount.set(0);

		Module module = new TestModuleEagerSingleton();
		Injector injector = Guice.createInjector(module);

		AssertJUnit.assertEquals(1, Impl.instancesCount.get());

		First first = injector.getInstance(First.class);
		Second second = injector.getInstance(Second.class);

		AssertJUnit.assertEquals(first, second);

		AssertJUnit.assertEquals(1, Impl.instancesCount.get());
	}

	@Test
	public void testSingletonAnnotatedImplLazy()
	{
		SingletonAnnotatedImpl.instancesCount.set(0);

		Module module = new TestModuleSingletonAnnotatedImplLazy();
		Injector injector = Guice.createInjector(module);

		AssertJUnit.assertEquals(0, SingletonAnnotatedImpl.instancesCount.get());

		First first = injector.getInstance(First.class);
		Second second = injector.getInstance(Second.class);

		AssertJUnit.assertEquals(first, second);

		AssertJUnit.assertEquals(1, SingletonAnnotatedImpl.instancesCount.get());
	}

	@Test
	public void testSingletonAnnotatedImplEager()
	{
		SingletonAnnotatedImpl.instancesCount.set(0);

		Module module = new TestModuleSingletonAnnotatedImplEager();
		Injector injector = Guice.createInjector(module);

		AssertJUnit.assertEquals(1, SingletonAnnotatedImpl.instancesCount.get());

		First first = injector.getInstance(First.class);
		Second second = injector.getInstance(Second.class);

		AssertJUnit.assertEquals(first, second);

		AssertJUnit.assertEquals(1, SingletonAnnotatedImpl.instancesCount.get());
	}

	public static class TestModuleEagerSingleton extends AbstractModule
	{
		@Override
		protected void configure() {
			bind(First.class).to(Impl.class);
			bind(Second.class).to(Impl.class);
			bind(Impl.class).asEagerSingleton();
		}
	}

	public static class TestModuleSingletonAnnotatedImplLazy extends AbstractModule
	{
		@Override
		protected void configure() {
			bind(First.class).to(SingletonAnnotatedImpl.class);
			bind(Second.class).to(SingletonAnnotatedImpl.class);
		}
	}

	public static class TestModuleSingletonAnnotatedImplEager extends AbstractModule
	{
		@Override
		protected void configure() {
			bind(First.class).to(SingletonAnnotatedImpl.class).asEagerSingleton();
			bind(Second.class).to(SingletonAnnotatedImpl.class).asEagerSingleton();
		}
	}

	public static class Impl implements First, Second
	{
		public static final AtomicInteger instancesCount = new AtomicInteger();

		public Impl()
		{
			instancesCount.incrementAndGet();
		}

		@Override
		public int getFirstValue()
		{
			return 0;
		}

		@Override
		public int getSecondValue()
		{
			return 0;
		}
	}

	@Singleton
	public static class SingletonAnnotatedImpl implements First, Second
	{
		public static final AtomicInteger instancesCount = new AtomicInteger();

		public SingletonAnnotatedImpl()
		{
			instancesCount.incrementAndGet();
		}

		@Override
		public int getFirstValue()
		{
			return 0;
		}

		@Override
		public int getSecondValue()
		{
			return 0;
		}
	}
}
