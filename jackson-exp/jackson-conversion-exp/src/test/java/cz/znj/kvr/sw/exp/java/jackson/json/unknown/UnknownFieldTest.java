package cz.znj.kvr.sw.exp.java.jackson.json.unknown;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import cz.znj.kvr.sw.exp.java.jackson.json.IgnoringTestObject;
import cz.znj.kvr.sw.exp.java.jackson.json.TestObject;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.IOException;


/**
 * Unknown property deserialization.
 */
public class UnknownFieldTest
{
	private ObjectMapper objectMapper = new ObjectMapper()
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

	@Test
	public void                     testCorrect() throws IOException
	{
		TestObject o = objectMapper.readValue("{ testId: 1, name: \"hello\" }", TestObject.class);
		AssertJUnit.assertEquals(1, o.getTestId());
		AssertJUnit.assertEquals("hello", o.getName());
	}

	@Test(expectedExceptions = UnrecognizedPropertyException.class)
	public void                     testUnknown() throws IOException
	{
		TestObject o = objectMapper.readValue("{ testId: 1, name: \"hello\", unknown: \"world\" }", TestObject.class);
		AssertJUnit.assertEquals(1, o.getTestId());
		AssertJUnit.assertEquals("hello", o.getName());
	}

	@Test
	public void                     testUnknownGlobalIgnore() throws IOException
	{
		ObjectMapper mapper = new ObjectMapper()
				.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		TestObject o = mapper.readValue("{ testId: 1, name: \"hello\", unknown: \"world\" }", TestObject.class);
		AssertJUnit.assertEquals(1, o.getTestId());
		AssertJUnit.assertEquals("hello", o.getName());
	}

	@Test
	public void                     testUnknownObjectIgnore() throws IOException
	{
		ObjectMapper mapper = new ObjectMapper()
				.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
		TestObject o = mapper.readValue("{ testId: 1, name: \"hello\", unknown: \"world\" }", IgnoringTestObject.class);
		AssertJUnit.assertEquals(1, o.getTestId());
		AssertJUnit.assertEquals("hello", o.getName());
	}}
