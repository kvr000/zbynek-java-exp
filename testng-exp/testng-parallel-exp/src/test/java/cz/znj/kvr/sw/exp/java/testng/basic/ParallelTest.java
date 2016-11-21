package cz.znj.kvr.sw.exp.java.testng.basic;

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
public class ParallelTest
{
	@BeforeClass
	public void init()
	{
		classWatch = Stopwatch.createStarted();
	}

	@Test
	public void test1() throws InterruptedException
	{
		Thread.sleep(500L);
	}

	@Test
	public void test2() throws InterruptedException
	{
		Thread.sleep(500L);
	}

	@Test
	public void test3() throws InterruptedException
	{
		Thread.sleep(500L);
	}

	@Test
	public void test4() throws InterruptedException
	{
		Thread.sleep(500L);
	}

	@AfterClass
	public void shutdown()
	{
		Assert.assertTrue("TestNg parallel run longer than single method run", classWatch.elapsed(TimeUnit.MILLISECONDS) < 1500);
	}

	private Stopwatch classWatch;
}
