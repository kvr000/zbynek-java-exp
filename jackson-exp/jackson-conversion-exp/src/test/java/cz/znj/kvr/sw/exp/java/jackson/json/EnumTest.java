package cz.znj.kvr.sw.exp.java.jackson.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Value;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.IOException;


public class EnumTest
{
	private ObjectMapper		objectMapper = new ObjectMapper()
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

	@Test
	public void                     testSerialization() throws IOException
	{
		TestObject o = TestObject.builder()
				.my(MyEnum.ONE)
				.build();

		String s = objectMapper.writeValueAsString(o);
		AssertJUnit.assertEquals("{\"my\":\"ONE\"}", s);
	}

	@Test
	public void                     testDeserialization() throws IOException
	{
		TestObject expected = TestObject.builder()
			.my(MyEnum.ONE)
			.build();

		TestObject actual = objectMapper.readValue("{\"my\":\"ONE\"}", TestObject.class);

		AssertJUnit.assertEquals(actual, expected);
	}

	@lombok.Builder(builderClassName = "Builder")
	@Value
	@JsonDeserialize(builder = TestObject.Builder.class)
	private static class TestObject
	{
		private final MyEnum my;

		@JsonPOJOBuilder(withPrefix = "")
		public static class Builder
		{
		}
	}

	public static enum MyEnum
	{
		ONE("one"),
		TWO("two");

		MyEnum(String description)
		{
			this.description = description;
		}

		private final String description;
	}
}
