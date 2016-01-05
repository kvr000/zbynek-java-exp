package cz.znj.kvr.sw.exp.java.gson.conversion;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

import java.util.List;


public class ConversionTest
{
	@Test
	public void                     testObject()
	{
		TestObject o = new Gson().fromJson("{ testId: 1, name: \"hello\" }", TestObject.class);
		Assert.assertEquals(1, o.getTestId());
		Assert.assertEquals("hello", o.getName());
	}

	@Test
	public void                     testList() throws NoSuchFieldException
	{
		List<TestObject> list = new Gson().fromJson("[{ testId: 1, name: \"hello\" }, {testId: 2, name: \"world\" }]", ConversionTest.class.getDeclaredField("testObjectListType").getGenericType());
		{
			TestObject o = list.get(0);
			Assert.assertEquals(1, o.getTestId());
			Assert.assertEquals("hello", o.getName());
		}
		{
			TestObject o = list.get(1);
			Assert.assertEquals(2, o.getTestId());
			Assert.assertEquals("world", o.getName());
		}
	}

	private static List<TestObject> testObjectListType;
}
