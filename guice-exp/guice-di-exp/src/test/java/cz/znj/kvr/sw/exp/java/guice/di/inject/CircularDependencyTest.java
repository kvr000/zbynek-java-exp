package cz.znj.kvr.sw.exp.java.guice.di.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.Test;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;


public class CircularDependencyTest
{
	@Test
	public void inject_multiWithQualifier_injectedTwo()
	{
		Injector injector = Guice.createInjector(new CircularModule());
		IntA intA = injector.getInstance(IntA.class);

		assertThat(intA, instanceOf(ClassA.class));
	}

	public static class CircularModule extends AbstractModule
	{
		@Override
		public void configure()
		{
			bind(IntA.class).to(ClassA.class).in(Singleton.class);
			bind(IntB.class).to(ClassB.class).in(Singleton.class);
		}
	}


	public static interface IntA  {}

	public static interface IntB  {}

	public static class ClassA implements IntA
	{
		@Inject
		public ClassA(IntB b)
		{
			this.b = b;
			// no call to b here, proxy only
		}

		private final IntB b;
	}

	public static class ClassB implements IntB
	{
		@Inject
		public ClassB(IntA a)
		{
			this.a = a;
			// no call to a here, proxy only
		}

		private final IntA a;
	}

}
