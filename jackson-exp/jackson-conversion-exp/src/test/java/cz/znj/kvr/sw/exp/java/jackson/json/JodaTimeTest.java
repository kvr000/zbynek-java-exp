package cz.znj.kvr.sw.exp.java.jackson.json;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import lombok.Getter;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.IOException;


public class JodaTimeTest
{
	private ObjectMapper		objectMapper = new ObjectMapper()
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
			.registerModule(new JodaModule());

	@Test
	public void                     testSerialization() throws IOException
	{
		TestObject o = TestObject.builder()
				.date(LocalDate.parse("1977-03-12"))
				.time(LocalTime.parse("06:05:04"))
				.build();
		String s = objectMapper.writeValueAsString(o);
		AssertJUnit.assertEquals("{\"date\":[1977,3,12],\"time\":[6,5,4,0]}", s);
	}

	@lombok.Builder(builderClassName = "Builder")
	@Getter
	@JsonPropertyOrder({ "date", "time" })
	@JsonPOJOBuilder(withPrefix = "")
	private static class TestObject
	{
		private final LocalDate date;
		private final LocalTime time;
	}
}
