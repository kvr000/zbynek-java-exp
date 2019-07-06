package cz.znj.kvr.sw.exp.java.corejava.lang.classload;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * The test is not thread safe, parallel testing must be disabled.
 */
public class LoadTest
{
	@Test
	public void testLazyClassLoad() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
	{
		Assert.assertFalse(loaded, "LoadTestClass only referenced from method yet");
		new LoadTestWrapper();
		Assert.assertFalse(loaded, "LoadTestClass only referenced from method yet");
		Method method = LoadTestWrapper.class.getMethod("method");
		Assert.assertFalse(loaded, "LoadTestClass only as a return type in reflection yet");
		Class<?> returnType = method.getReturnType();
		Assert.assertFalse(loaded, "LoadTestClass only passively referenced yet");
		returnType.getName();
		Assert.assertFalse(loaded, "LoadTestClass being checked for name, not yet initialized");
		returnType.isInterface();
		Assert.assertFalse(loaded, "LoadTestClass being checked for type, not yet initialized");
		returnType.getConstructor();
		Assert.assertFalse(loaded, "LoadTestClass being checked for constructor, not yet initialized");
		returnType.getConstructor();
		Assert.assertFalse(loaded, "LoadTestClass being checked for constructor, not yet initialized");
		returnType.getConstructor().newInstance();
		Assert.assertTrue(loaded, "LoadTestClass instance created, initialized");
	}

	static boolean loaded = false;
}
