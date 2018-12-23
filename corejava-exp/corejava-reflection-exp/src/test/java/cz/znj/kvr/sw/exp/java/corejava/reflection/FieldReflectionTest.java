package cz.znj.kvr.sw.exp.java.corejava.reflection;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;


/**
 * Test various ways of getting fields using reflection.
 *
 * @author
 * 	Zbynek Vyskovsky
 */
public class FieldReflectionTest
{
	@Test
	public void testGetRegular() throws NoSuchFieldException, IllegalAccessException
	{
		TestClass testClass = new TestClass();

		Field regularField = testClass.getClass().getDeclaredField("regularField");
		regularField.setAccessible(true);

		AssertJUnit.assertEquals("regular", regularField.get(testClass));
	}

	@Test
	public void testGetFinal() throws NoSuchFieldException, IllegalAccessException
	{
		TestClass testClass = new TestClass();

		Field finalField = testClass.getClass().getDeclaredField("finalField");
		finalField.setAccessible(true);

		AssertJUnit.assertEquals("final", finalField.get(testClass));
	}

	public static class TestClass
	{
		private String regularField = "regular";

		private final String finalField = "final";
	}
}
