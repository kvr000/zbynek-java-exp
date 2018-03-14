package cz.znj.kvr.sw.exp.java.jackson.json.custom;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import lombok.Builder;
import lombok.Value;
import org.testng.annotations.Test;


/**
 * Test for custom deserialization.
 */
public class CustomAdjustDeserializationTest
{
	private ObjectMapper mapper = new ObjectMapper()
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

	/**
	 * Simple way of adjusting value after it has been read by Jackson, unfortunately does not work now. Probably need to provide full custom deserializer.
	 *
	 * @throws Exception
	 * 	when error
	 */
	@Test
	public void testCustomDeserialization() throws Exception
	{
		TestObject o = mapper.readValue("{ value: 1, name: \"hello\" }", TestObject.class);
		//AssertJUnit.assertEquals(2, o.getValue());
	}

	@Value
	@Builder(builderClassName = "Builder", toBuilder = true)
	@JsonDeserialize(builder = TestObject.Builder.class, converter = TestObjectConverter.class, contentConverter = TestObjectConverter.class)
	public static class TestObject
	{
		private String name;

		private int value;

		@JsonPOJOBuilder(withPrefix = "")
		@JsonDeserialize(builder = TestObject.Builder.class, converter = TestObjectConverter.class, contentConverter = TestObjectConverter.class)
		public static class Builder
		{
		}
	}

	public static class TestObjectConverter implements Converter<TestObject.Builder, TestObject>
	{
		private static final JavaType testObjectType = TypeFactory.defaultInstance().constructType(TestObject.class);
		private static final JavaType testObjectBuilderType = TypeFactory.defaultInstance().constructType(TestObject.Builder.class);

		@Override
		public TestObject convert(TestObject.Builder value)
		{
			return value.value(value.value+1).build();
		}

		@Override
		public JavaType getInputType(TypeFactory typeFactory)
		{
			return testObjectType;
		}

		@Override
		public JavaType getOutputType(TypeFactory typeFactory)
		{
			return testObjectType;
		}
	}
}
