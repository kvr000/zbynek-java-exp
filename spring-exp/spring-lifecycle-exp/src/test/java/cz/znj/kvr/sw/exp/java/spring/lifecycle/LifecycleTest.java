package cz.znj.kvr.sw.exp.java.spring.lifecycle;

import org.junit.Assert;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


@ContextConfiguration(classes = Object.class)
public class LifecycleTest extends AbstractTestNGSpringContextTests implements InitializingBean
{
	private int initCounter = 0;

	@PostConstruct
	public LifecycleTest init()
	{
		System.err.println("Initializing.");
		Assert.assertEquals(0, initCounter);
		initCounter = 1;
		return this;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.err.println("After properties set.");
		Assert.assertEquals(1, initCounter);
		initCounter = 10;
	}

	@Test
	public void testLifecycle()
	{
		System.err.println("Running test");
		Assert.assertEquals(10, initCounter);
		initCounter = 20;
	}

	@PreDestroy
	public void destroy()
	{
		System.err.println("Destroying test object");
		Assert.assertEquals(20, initCounter);
	}

	@AfterClass
	public void destroyTest()
	{
		System.err.println("After test object");
		Assert.assertEquals(20, initCounter);
	}
}
