package cz.znj.kvr.sw.exp.java.jackson.protobuf;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufFactory;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;


public class ProtobufConversionTest
{
	private ObjectMapper		objectMapper = new ObjectMapper(new ProtobufFactory())
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

	@Test(expectedExceptions = JsonParseException.class)
	public void                     testObject() throws IOException
	{
		cz.znj.kvr.sw.exp.java.jackson.json.TestObject o = objectMapper.readValue("{ testId: 1, name: \"hello\" }".getBytes(), cz.znj.kvr.sw.exp.java.jackson.json.TestObject.class);
		AssertJUnit.assertEquals(1, o.getTestId());
		AssertJUnit.assertEquals("hello", o.getName());
	}

	@Test(expectedExceptions = JsonParseException.class)
	public void                     testList() throws IOException
	{
		List<TestObject> list = objectMapper.readValue("[{ testId: 1, name: \"hello\" }, {testId: 2, name: \"world\" }]".getBytes(), new TypeReference<List<TestObject>>(){});
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

	@Test(expectedExceptions = JsonMappingException.class)
	public void			testSerialize() throws IOException
	{
		TestObject o = TestObject.builder()
				.name("Hello")
				.testId(9)
				.build();
		byte[] bytes = objectMapper.writeValueAsBytes(o);
		AssertJUnit.assertNotNull(bytes);
	}
}
