package cz.znj.kvr.sw.exp.java.corejava.reflection;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodOverrideReflectionTest
{
	@Test
	public void testOverrideCall() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
	{
		Method m = A.class.getMethod("myMethod");

		String resultA = (String) m.invoke(new B());
		Assert.assertEquals("B", resultA);

		String resultB = (String) m.invoke(new B());
		Assert.assertEquals("B", resultB);
	}

	public static class A
	{
		public String myMethod()
		{
			return "A";
		}
	}

	public static class B extends A
	{
		public String myMethod()
		{
			return "B";
		}
	}
}
