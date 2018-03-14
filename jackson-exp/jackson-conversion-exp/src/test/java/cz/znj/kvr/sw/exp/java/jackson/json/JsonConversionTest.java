package cz.znj.kvr.sw.exp.java.jackson.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;


public class JsonConversionTest
{
	private ObjectMapper		objectMapper = new ObjectMapper()
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

	@Test
	public void                     testObject() throws IOException
	{
		TestObject o = objectMapper.readValue("{ testId: 1, name: \"hello\" }", TestObject.class);
		AssertJUnit.assertEquals(1, o.getTestId());
		AssertJUnit.assertEquals("hello", o.getName());
	}

	@Test
	public void                     testList() throws IOException
	{
		List<TestObject> list = objectMapper.readValue("[{ testId: 1, name: \"hello\" }, {testId: 2, name: \"world\" }]", new TypeReference<List<TestObject>>(){});
		{
			TestObject o = list.get(0);
			AssertJUnit.assertEquals(1, o.getTestId());
			AssertJUnit.assertEquals("hello", o.getName());
		}
		{
			TestObject o = list.get(1);
			AssertJUnit.assertEquals(2, o.getTestId());
			AssertJUnit.assertEquals("world", o.getName());
		}
	}

	@Test
	public void			testTrailingCommas() throws IOException
	{
		ObjectMapper mapper = new ObjectMapper()
				.enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);

		SingleFieldObject o = new SingleFieldObject();
		o.setTestId(17);

		AssertJUnit.assertEquals("{\"testId\":17}", mapper.writeValueAsString(o));

		o = mapper.readValue("{\"testId\":21}", SingleFieldObject.class);
		AssertJUnit.assertEquals(21, o.getTestId());
	}
}
