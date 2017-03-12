package cz.znj.kvr.sw.exp.java.jackson.builder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;


/**
 * Test of Lombok created Builder.
 */
public class LombokBuilderTest
{
	private ObjectMapper objectMapper = new ObjectMapper()
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

	@Test
	public void                     testObject() throws IOException
	{
		TestObject o = objectMapper.readValue("{ testId: 1, name: \"hello\" }", TestObject.class);
		Assert.assertEquals(1, o.getTestId());
		Assert.assertEquals("hello", o.getName());
		TestObject.builder().build();
	}
}
