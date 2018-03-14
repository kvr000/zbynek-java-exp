package cz.znj.kvr.sw.exp.java.jackson.json.nested;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;


/**
 *
 */
public class NestedTest
{
	@Test
	public void testSerialization() throws JsonProcessingException
	{
		RootMapObject o = new RootMapObject();
		o.setName("Zbynek Vyskovsky");
		o.setProperties(Collections.singletonMap("birth", "Czech"));
		String serialized = new ObjectMapper().writeValueAsString(o);
		AssertJUnit.assertEquals("{\"name\":\"Zbynek Vyskovsky\",\"birth\":\"Czech\"}", serialized);
	}

	@Data
	@JsonPropertyOrder({ "name", "properties" })
	public static class RootMapObject
	{
		private String name;

		Map<String, String> properties;

		@JsonProperty("map_")
		@JsonAnyGetter
		public Map<String, String> getProperties()
		{
			return this.properties;
		}
	}
}
