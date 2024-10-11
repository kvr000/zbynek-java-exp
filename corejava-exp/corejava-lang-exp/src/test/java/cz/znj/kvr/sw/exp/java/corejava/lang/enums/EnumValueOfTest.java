package cz.znj.kvr.sw.exp.java.corejava.lang.enums;

import com.google.common.collect.ImmutableMap;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;


/**
 * Enum tests.
 */
public class EnumValueOfTest
{
	@Test
	public void valueOf_whenNotExist_throws() throws Exception
	{
		Assert.assertThrows(IllegalArgumentException.class, () -> MyEnum.valueOf("Three"));
	}

	@Test
	public void index_whenNotExist_returnNull() throws Exception
	{
		var value3 = MyEnum.INDEX.get("Three");
		Assert.assertNull(value3);
		var value2 = MyEnum.INDEX.get("Two");
		Assert.assertEquals(value2, MyEnum.Two);
	}

	enum MyEnum
	{
		One,
		Two;

		public static final Map<String, MyEnum> INDEX =
				Arrays.stream(MyEnum.values()).collect(ImmutableMap.toImmutableMap(Enum::name, Function.identity()));
	}
}
