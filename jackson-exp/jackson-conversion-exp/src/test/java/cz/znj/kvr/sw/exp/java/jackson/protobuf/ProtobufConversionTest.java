package cz.znj.kvr.sw.exp.java.jackson.protobuf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;


public class ProtobufConversionTest
{
	private ObjectMapper		objectMapper = new ObjectMapper(new ProtobufFactory())
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

	@Test
	public void                     testObject() throws IOException
	{
		cz.znj.kvr.sw.exp.java.jackson.json.TestObject o = objectMapper.readValue("{ testId: 1, name: \"hello\" }".getBytes(), cz.znj.kvr.sw.exp.java.jackson.json.TestObject.class);
		Assert.assertEquals(1, o.getTestId());
		Assert.assertEquals("hello", o.getName());
	}

	@Test
	public void                     testList() throws IOException
	{
		List<TestObject> list = objectMapper.readValue("[{ testId: 1, name: \"hello\" }, {testId: 2, name: \"world\" }]".getBytes(), new TypeReference<List<cz.znj.kvr.sw.exp.java.jackson.json.TestObject>>(){});
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

	@Test
	public void			testSerialize() throws IOException
	{
		TestObject o = TestObject.builder()
				.name("Hello")
				.testId(9)
				.build();
		byte[] bytes = objectMapper.writeValueAsBytes(o);
		Assert.assertNotNull(bytes);
	}
}
