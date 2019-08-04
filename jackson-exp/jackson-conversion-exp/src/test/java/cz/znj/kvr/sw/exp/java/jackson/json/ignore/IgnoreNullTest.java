package cz.znj.kvr.sw.exp.java.jackson.json.ignore;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * Tests ignoring null fields.
 */
public class IgnoreNullTest
{
	ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new Jdk8Module());

	@Test
	public void testIgnoreNull() throws JsonProcessingException
	{
		IgnoreNullClass o = IgnoreNullClass.builder()
				.x("hello")
				.build();

		String s = objectMapper.writeValueAsString(o);

		Assert.assertEquals(s, "{\"x\":\"hello\"}");
	}

	@Test
	public void testIgnoreAbsent() throws JsonProcessingException
	{
		IgnoreAbsentClass o = IgnoreAbsentClass.builder()
				.x(Optional.of("hello"))
				.build();

		String s = objectMapper.writeValueAsString(o);

		Assert.assertEquals(s, "{\"x\":\"hello\"}");
	}

	@Test
	public void testDeserializeIgnoreAbsent() throws IOException
	{
		IgnoreAbsentClass o = objectMapper.readValue("{\"x\":\"hello\"}", IgnoreAbsentClass.class);

		Assert.assertEquals(o.x, Optional.of("hello"));
		Assert.assertEquals(o.y, Optional.empty());
	}

	@Test
	public void testIgnoreEmpty() throws JsonProcessingException
	{
		IgnoreEmptyClass o = IgnoreEmptyClass.builder()
				.nullList(null)
				.emptyList(Collections.emptyList())
				.fullList(Collections.singletonList("hello"))
				.build();

		String s = objectMapper.writeValueAsString(o);

		Assert.assertEquals(s, "{\"fullList\":[\"hello\"]}");
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Data
	@Builder()
	public static class IgnoreNullClass
	{
		private String x;
		private String y;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Data
	@Builder()
	@NoArgsConstructor
	@AllArgsConstructor
	public static class IgnoreAbsentClass
	{
		private Optional<String> x = Optional.empty();
		private Optional<String> y = Optional.empty();
	}

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@Data
	@Builder()
	@NoArgsConstructor
	@AllArgsConstructor
	public static class IgnoreEmptyClass
	{
		private List<String> nullList;
		private List<String> emptyList;
		private List<String> fullList;
	}
}
