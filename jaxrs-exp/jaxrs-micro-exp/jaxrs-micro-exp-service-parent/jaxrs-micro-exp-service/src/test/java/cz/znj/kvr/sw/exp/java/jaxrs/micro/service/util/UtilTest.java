package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.util;

import com.google.common.collect.ImmutableList;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * {@link Util} tests.
 */
public class UtilTest
{
	@Test
	public void splitByCharTestEmpty()
	{
		Iterable<String> result = Util.splitByChar("", '-');
		Assert.assertEquals(ImmutableList.copyOf(result), ImmutableList.of());
	}

	@Test
	public void splitByCharTestMiddle()
	{
		Iterable<String> result = Util.splitByChar("a-b", '-');
		Assert.assertEquals(ImmutableList.copyOf(result), ImmutableList.of("a", "b"));
	}

	@Test
	public void splitByCharTestBeginning()
	{
		Iterable<String> result = Util.splitByChar("-b", '-');
		Assert.assertEquals(ImmutableList.copyOf(result), ImmutableList.of("", "b"));
	}

	@Test
	public void splitByCharTestEnd()
	{
		Iterable<String> result = Util.splitByChar("a-", '-');
		Assert.assertEquals(ImmutableList.copyOf(result), ImmutableList.of("a", ""));
	}

	@Test
	public void splitByCharTestMiddleEmpty()
	{
		Iterable<String> result = Util.splitByChar("a--b", '-');
		Assert.assertEquals(ImmutableList.copyOf(result), ImmutableList.of("a", "", "b"));
	}
}
