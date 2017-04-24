package cz.znj.kvr.sw.exp.java.jackson.json;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;


public class Java8TimeTest
{
	private ObjectMapper		objectMapper = new ObjectMapper()
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
			.registerModule(new Jdk8Module())
			.registerModule(new JavaTimeModule());

	@Test
	public void                     testSerialization() throws IOException
	{
		TestObject o = TestObject.builder()
				.date(LocalDate.of(1977, 03, 12))
				.time(LocalTime.of(06, 05, 04))
				.build();
		String s = objectMapper.writeValueAsString(o);
		Assert.assertEquals("{\"date\":[1977,3,12],\"time\":[6,5,4]}", s);
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
