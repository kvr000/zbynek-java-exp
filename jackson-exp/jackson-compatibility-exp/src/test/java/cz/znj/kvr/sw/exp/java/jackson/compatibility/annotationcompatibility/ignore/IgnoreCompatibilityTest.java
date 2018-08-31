package cz.znj.kvr.sw.exp.java.jackson.compatibility.annotationcompatibility.ignore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.Map;


/**
 *
 */
public class IgnoreCompatibilityTest
{
	public final static ObjectMapper mapper = new ObjectMapper()
			.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

	@Test
	public void testIgnore1()
	{
		TestObject1 o = new TestObject1();
		Map<String, Object> map = mapper.convertValue(o, new TypeReference<Map<String, Object>>(){});

		AssertJUnit.assertTrue(map.containsKey("age"));
	}

	@Test
	public void testIgnore2()
	{
		TestObject2 o = new TestObject2();
		Map<String, Object> map = mapper.convertValue(o, new TypeReference<Map<String, Object>>(){});

		AssertJUnit.assertFalse(map.containsKey("age"));
	}

}
