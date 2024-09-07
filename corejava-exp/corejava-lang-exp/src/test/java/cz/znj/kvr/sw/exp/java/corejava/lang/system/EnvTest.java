package cz.znj.kvr.sw.exp.java.corejava.lang.system;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Reading environment.
 */
public class EnvTest
{
	@Test
	public void readEnvironmentTest()
	{
		Assertions.assertEquals("thevalue123", System.getenv("myenvtest"));
	}

	@Test
	public void readEnvironmentAsPropertyTest()
	{
		Assertions.assertEquals(null, System.getProperty("myenvtest"));
	}

	@Test
	public void readPropertyTest()
	{
		Assertions.assertEquals("thevalue321", System.getProperty("myproptest"));
	}
}
