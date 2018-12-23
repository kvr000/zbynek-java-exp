package cz.znj.kvr.sw.exp.java.corejava.annotation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author
 * 	Zbynek Vyskovsky
 */
public class AnnotationInheritanceTest
{
	@Test
	public void testAnnotationInheritanceClass()
	{
		AssertJUnit.assertNotNull(BaseClass.class.getAnnotation(SimpleAnnotation.class));
		AssertJUnit.assertNotNull(BaseClass.class.getAnnotation(InheritedAnnotation.class));

		AssertJUnit.assertNull(ChildClass.class.getAnnotation(SimpleAnnotation.class));
		AssertJUnit.assertNotNull(ChildClass.class.getAnnotation(InheritedAnnotation.class));
	}

	@Test
	public void testAnnotationInheritanceMethod() throws NoSuchMethodException
	{
		AssertJUnit.assertNotNull(BaseClass.class.getMethod("method").getAnnotation(SimpleAnnotation.class));
		AssertJUnit.assertNotNull(BaseClass.class.getMethod("method").getAnnotation(InheritedAnnotation.class));
		AssertJUnit.assertNotNull(BaseClass.class.getMethod("rootMethod").getAnnotation(SimpleAnnotation.class));
		AssertJUnit.assertNotNull(BaseClass.class.getMethod("rootMethod").getAnnotation(InheritedAnnotation.class));

		AssertJUnit.assertNull(ChildClass.class.getMethod("method").getAnnotation(SimpleAnnotation.class));
		AssertJUnit.assertNull(ChildClass.class.getMethod("method").getAnnotation(InheritedAnnotation.class));
		AssertJUnit.assertNotNull(ChildClass.class.getMethod("rootMethod").getAnnotation(SimpleAnnotation.class));
		AssertJUnit.assertNotNull(ChildClass.class.getMethod("rootMethod").getAnnotation(InheritedAnnotation.class));
	}

	@SimpleAnnotation
	@InheritedAnnotation
	public static class BaseClass
	{
		@SimpleAnnotation
		@InheritedAnnotation
		public void rootMethod()
		{
		}

		@SimpleAnnotation
		@InheritedAnnotation
		public void method()
		{
		}
	}

	public static class ChildClass extends BaseClass
	{
		@Override
		public void method()
		{
		}
	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SimpleAnnotation
	{
	}

	@Inherited
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InheritedAnnotation
	{
	}
}
