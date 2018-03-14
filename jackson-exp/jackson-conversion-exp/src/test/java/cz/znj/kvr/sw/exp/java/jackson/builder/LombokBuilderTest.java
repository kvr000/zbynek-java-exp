package cz.znj.kvr.sw.exp.java.jackson.builder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

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
		AssertJUnit.assertEquals(1, o.getTestId());
		AssertJUnit.assertEquals("hello", o.getName());
		TestObject.builder().build();
	}
}
