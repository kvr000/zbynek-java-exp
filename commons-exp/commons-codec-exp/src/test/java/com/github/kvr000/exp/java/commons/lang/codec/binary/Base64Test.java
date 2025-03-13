package com.github.kvr000.exp.java.commons.lang.codec.binary;

import org.apache.commons.codec.binary.Base64;
import org.testng.annotations.Test;
import org.testng.internal.junit.ArrayAsserts;

import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;


public class Base64Test
{
	@Test
	public void decode_normal_return()
	{
		Base64 base64 = Base64.builder().get();
		byte[] result = base64.decode("ABCD");
		assertArrayEquals(result, new byte[]{ 0, 16, -125 });
	}

	@Test
	public void decode_withNewLine_acceptsNewLine()
	{
		Base64 base64 = Base64.builder().get();
		byte[] result = base64.decode("ABCD\n");
		assertArrayEquals(result, new byte[]{ 0, 16, -125 });
	}

	@Test
	public void decodeBase64_withNewLine_acceptsNewLine()
	{
		byte[] result = Base64.decodeBase64("ABCD\n");
		assertArrayEquals(result, new byte[]{ 0, 16, -125 });
	}

	@Test
	public void decodeBase64_multiline_acceptsNewLine()
	{
		byte[] result = Base64.decodeBase64("ABCD\nABCD");
		assertArrayEquals(result, new byte[]{ 0, 16, -125, 0, 16, -125 });
	}
}
