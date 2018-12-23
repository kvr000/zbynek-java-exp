package cz.znj.kvr.sw.exp.java.logexp.log4j2.logging.formatting;

import lombok.extern.log4j.Log4j2;
import org.testng.annotations.Test;


/**
 * Test object for JsonIgnore testing.
 */
@Log4j2
public class PlaceholderTest
{
	@Test
	public void logWithPlaceholder()
	{
		log.fatal("Hello: {}", "World");
	}

	/**
	 * Testing numbered placeholders. They DO NOT actually work in Log4j2.
	 */
	@Test
	public void logWithNumberedPlaceHolder()
	{
		log.fatal("Placeholder: {1} {0}", "World", "Hello");
	}
}
