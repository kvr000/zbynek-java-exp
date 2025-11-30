package cz.znj.kvr.sw.exp.java.guice.di.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import org.testng.annotations.Test;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;


public class QualifierTest
{
	@Test
	public void inject_multiWithQualifier_injectedTwo()
	{
		Injector injector = Guice.createInjector(new QualifierModule());
		Component component = injector.getInstance(Component.class);

		assertThat(component.a, instanceOf(ClassA.class));
		assertThat(component.b, instanceOf(ClassB.class));
	}

	public static class QualifierModule extends AbstractModule
	{
		@Override
		public void configure()
		{
			bind(Key.get(Injected.class, ResourceA.class)).to(ClassA.class).in(Singleton.class);
			bind(Component.class).in(Singleton.class);
		}

		@Provides
		@Singleton
		@ResourceB
		public Injected injectedB()
		{
			return new ClassB();
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	public @interface ResourceA {}

	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	public @interface ResourceB {}

	public interface Injected {}

	public static class ClassA implements Injected {}

	public static class ClassB implements Injected {}

	public static class Component
	{
		@Inject
		public Component(@ResourceA Injected a, @ResourceB Injected b)
		{
			this.a = a;
			this.b = b;
		}

		@ResourceA
		private final Injected a;
		@ResourceB
		private final Injected b;
	}
}
