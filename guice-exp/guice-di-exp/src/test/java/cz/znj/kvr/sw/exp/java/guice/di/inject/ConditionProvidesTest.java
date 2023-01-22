package cz.znj.kvr.sw.exp.java.guice.di.inject;

import com.google.common.base.Preconditions;
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
import lombok.RequiredArgsConstructor;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;


public class ConditionProvidesTest
{
	@Test
	public void testInjectOne()
	{
		Module module = new ConditionModule(1);
		Injector injector = Guice.createInjector(module);
		Conditional conditional = injector.getInstance(Conditional.class);
		assertThat(conditional, instanceOf(ConditionalOne.class));
	}

	@Test
	public void testInjectTwo()
	{
		Module module = new ConditionModule(2);
		Injector injector = Guice.createInjector(module);
		Conditional conditional = injector.getInstance(Conditional.class);
		assertThat(conditional, instanceOf(ConditionalTwo.class));
	}

	@RequiredArgsConstructor
	public class ConditionModule extends AbstractModule
	{
		private final int route;

		@Override
		protected void configure()
		{
		}

		@Provides
		@Singleton
		@Inject
		public ConditionalOne conditionalOne(FactoryOne factory)
		{
			Preconditions.checkArgument(route == 1);
			return new ConditionalOne();
		}

		@Provides
		@Singleton
		@Inject
		public ConditionalTwo conditionalTwo(FactoryTwo factory)
		{
			Preconditions.checkArgument(route == 2);
			return new ConditionalTwo();
		}

		@Provides
		@Singleton
		public Conditional conditional(Injector injector)
		{
			return injector.getInstance((Class<? extends Conditional>) (route == 1 ? ConditionalOne.class : ConditionalTwo.class));
		}
	}

	public interface Conditional
	{
	}

	public static class ConditionalOne implements Conditional
	{
	}

	public static class ConditionalTwo implements Conditional
	{
	}

	public static class FactoryOne
	{
	}

	public static class FactoryTwo
	{
	}
}
