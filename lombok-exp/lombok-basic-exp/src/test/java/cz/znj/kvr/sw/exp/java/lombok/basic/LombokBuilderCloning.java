package cz.znj.kvr.sw.exp.java.lombok.basic;

import lombok.Builder;
import lombok.Getter;
import org.junit.Assert;
import org.testng.annotations.Test;


/**
 * @author
 * 	Zbynek Vyskovsky
 */
public class LombokBuilderCloning
{
	@Test
	public void testBuilder()
	{
		TestObject to = TestObject.builder()
				.value(1)
				.title("Hello")
				.build();
		TestObject tc = to.toBuilder()
				.value(2)
				.build();
		Assert.assertEquals(2, tc.getValue());
		Assert.assertEquals("Hello", tc.getTitle());
	}

	@Builder(toBuilder = true)
	@Getter
	public static class TestObject
	{
		private int value;

		private String title;
	}
}
