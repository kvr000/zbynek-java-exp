package cz.znj.kvr.sw.exp.java.commons.beanutils;

import org.apache.commons.beanutils.ConvertUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Date;
import java.util.List;


public class ConvertUtilsTest
{
	@Test
	public void			testConversions()
	{
		Assert.assertEquals(0, ConvertUtils.convert("0", Integer.class));
		Assert.assertEquals(10, ConvertUtils.convert("10", Integer.class));
		Assert.assertEquals(-10, (int)ConvertUtils.convert("-10", int.class));
		Assert.assertEquals(false, ConvertUtils.convert("false", boolean.class));
		Assert.assertEquals(true, ConvertUtils.convert("true", boolean.class));
		Assert.assertEquals(true, ConvertUtils.convert("1", boolean.class));
		Assert.assertEquals("hello", ConvertUtils.convert("hello", String.class));
		Assert.assertEquals(false, ConvertUtils.convert(0, boolean.class));
		Assert.assertEquals(true, ConvertUtils.convert(1, boolean.class));
		Assert.assertEquals(new Date(36000_000), ConvertUtils.convert("1970-01-01T10:00:00", Date.class));
	}
}
