package cz.znj.kvr.sw.exp.java.testng.multifw;

import com.google.common.base.Stopwatch;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;


/**
 * @author
 * 	Zbyněk Vyškovský
 */
@Log4j2
@Test(groups =  "unit")
public class TestngTest
{
	@Test
	public void testNg()
	{
		log.error("Hello from testNg");
	}
}
