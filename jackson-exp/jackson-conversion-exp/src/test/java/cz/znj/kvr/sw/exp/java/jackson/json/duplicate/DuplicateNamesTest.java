package cz.znj.kvr.sw.exp.java.jackson.json.duplicate;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;


/**
 * Testing duplicated values of different names.
 */
public class DuplicateNamesTest
{
	private final ObjectMapper objectMapper = new ObjectMapper()
		.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

	@Test
	public void testDuplicateSerialization()
	{
		DuplicateFieldsObject obj = new DuplicateFieldsObject();
		obj.isValue = true;
		Map<String, Object> result = objectMapper.convertValue(obj, Map.class);
		Assert.assertEquals(ImmutableMap.of("value", true, "isValue", true), result);
	}

	/**
	 * Check both values are consumed and the last one has precedence.
	 */
	@Test
	public void testDuplicateDeserialization() throws IOException
	{
		Map<String, Object> input = ImmutableMap.of("value", false, "isValue", true);
		DuplicateFieldsObject obj = objectMapper.convertValue(input, DuplicateFieldsObject.class);
		Assert.assertEquals(true, obj.isValue());
		Map<String, Object> inputOpposite = ImmutableMap.of("isValue", false, "value", true);
		DuplicateFieldsObject objOpposite = objectMapper.convertValue(input, DuplicateFieldsObject.class);
		Assert.assertEquals(true, objOpposite.isValue());
	}

	@Test
	public void testSerializeObject() throws IOException
	{
		ObjectMapper customMapper = new ObjectMapper()
			.registerModule(new SimpleModule()
				.addSerializer(new DuplicateFieldsSerializer())
			);
		DuplicateFieldsObject obj = new DuplicateFieldsObject();
		obj.isValue = true;
		Map<String, Object> result = customMapper.convertValue(obj, Map.class);
		Assert.assertEquals(ImmutableMap.of("value", true, "isValue", true, "moreValue", true), result);
	}

	@Data
	public static class DuplicateFieldsObject
	{
		@JsonProperty("isValue")
		boolean isValue;

		@JsonGetter("value")
		public boolean _dupget_value()
		{
			return isValue;
		}
	}

	public static class DuplicateFieldsSerializer extends StdSerializer<DuplicateFieldsObject>
	{
		private static final JavaType THE_TYPE =
			TypeFactory.defaultInstance().constructType(DuplicateFieldsObject.class);
		private volatile JsonSerializer<Object> delegateSerializer;

		protected DuplicateFieldsSerializer()
		{
			super(DuplicateFieldsObject.class);
		}

		@Override
		public void serialize(DuplicateFieldsObject value, JsonGenerator jgen, SerializerProvider provider) throws IOException
		{
			if (delegateSerializer == null) {
				BeanDescription beanDesc = provider.getConfig().introspect(THE_TYPE);
				JsonSerializer<Object> serializer = BeanSerializerFactory.instance.findBeanSerializer(
					provider, THE_TYPE, beanDesc);
				delegateSerializer = serializer.unwrappingSerializer(null);
			}
			jgen.writeStartObject();
			delegateSerializer.unwrappingSerializer(null).serialize(value, jgen, provider);
			jgen.writeObjectField("moreValue", value.isValue());
			jgen.writeEndObject();
		}
	}
}
