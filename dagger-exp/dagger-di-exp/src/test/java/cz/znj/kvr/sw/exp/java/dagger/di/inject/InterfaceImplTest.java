package cz.znj.kvr.sw.exp.java.dagger.di.inject;

import cz.znj.kvr.sw.exp.java.dagger.di.common.dagger.DaggerInterfaceImplComponent;
import cz.znj.kvr.sw.exp.java.dagger.di.common.dagger.InterfaceImplComponent;
import org.reflections.Reflections;
import org.reflections.scanners.MethodParameterScanner;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;


/**
 *
 */
public class InterfaceImplTest
{
	@Test
	public void testInterface()
	{
		Fixture f = new Fixture();
		Reflections reflections = new Reflections(f.dagger.getClass(), new MethodParameterScanner());
		Set<Method> methods = reflections.getMethodsMatchParams().stream()
				.filter(m ->
						(m.getModifiers()&(Modifier.PUBLIC|Modifier.STATIC)) ==(Modifier.PUBLIC) &&
								m.getDeclaringClass() == DaggerInterfaceImplComponent.class
				)
				.collect(Collectors.toSet());
		Assert.assertEquals(methods.size(), 3);
	}

	public static class Fixture
	{
		private final InterfaceImplComponent dagger = DaggerInterfaceImplComponent.create();
	}
}
