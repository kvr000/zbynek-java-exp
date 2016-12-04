package cz.znj.kvr.sw.exp.java.lombok.basic;

import lombok.Builder;
import lombok.Generated;
import lombok.Getter;
import org.junit.Assert;
import org.testng.annotations.Test;


/**
 * @author
 * 	Zbynek Vyskovsky
 */
public class LombokBuilder
{
	@Test
	public void testBuilder()
	{
		TestObject to = TestObject.builder()
				.value(1)
				.title("Hello")
				.build();
		Assert.assertEquals(1, to.getValue());
		Assert.assertEquals("Hello", to.getTitle());
	}

	@Builder
	@Getter
	public static class TestObject
	{
		private int value;

		private String title;
	}
}
