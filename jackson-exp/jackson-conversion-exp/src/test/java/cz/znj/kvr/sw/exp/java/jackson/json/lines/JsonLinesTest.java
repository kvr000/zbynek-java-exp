package cz.znj.kvr.sw.exp.java.jackson.json.lines;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Value;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;


public class JsonLinesTest
{
	private ObjectMapper objectMapper = new ObjectMapper()
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
			.registerModule(new Jdk8Module())
			.registerModule(new JavaTimeModule());

	@Test
	public void testLines() throws IOException
	{
		try (InputStream input = JsonLinesTest.class.getResourceAsStream("JsonLines-lines.json");
			MappingIterator<TestObject> iterator = objectMapper.readerFor(TestObject.class).readValues(input)) {
			TestObject o0 = iterator.next();
			Assert.assertEquals(o0.getName(), "Zbynek");
			TestObject o1 = iterator.next();
			Assert.assertEquals(o1.getName(), "Vyskovsky");
			Assert.assertFalse(iterator.hasNext());
		}
	}

	@Test
	public void testMultiLines() throws IOException
	{
		try (InputStream input = JsonLinesTest.class.getResourceAsStream("JsonLines-multilines.json");
			 MappingIterator<TestObject> iterator = objectMapper.readerFor(TestObject.class).readValues(input)) {
			TestObject o0 = iterator.next();
			Assert.assertEquals(o0.getName(), "Multi");
			TestObject o1 = iterator.next();
			Assert.assertEquals(o1.getName(), "Line");
			Assert.assertFalse(iterator.hasNext());
		}
	}

	@Test
	public void testArray() throws IOException
	{
		try (InputStream input = JsonLinesTest.class.getResourceAsStream("JsonLines-array.json");
			 MappingIterator<TestObject> iterator = objectMapper.readerFor(TestObject.class).readValues(input)) {
			TestObject o0 = iterator.next();
			Assert.assertEquals(o0.getName(), "Zero");
			TestObject o1 = iterator.next();
			Assert.assertEquals(o1.getName(), "One");
			Assert.assertFalse(iterator.hasNext());
		}
	}

	@lombok.Builder(builderClassName = "Builder", toBuilder = true)
	@Value
	@JsonDeserialize(builder = TestObject.Builder.class)
	private static class TestObject
	{
		String name;

		@JsonPOJOBuilder(withPrefix = "")
		public static class Builder
		{
		}
	}
}
