package cz.znj.kvr.sw.exp.java.spring.di.importdi;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import jakarta.inject.Inject;


@ContextConfiguration(classes = ImportDiTest.MainConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ImportDiTest
{
	@Inject
	private String childBean;

	@Test
	public void testImport()
	{
		Assert.assertNotNull(childBean);
		Assert.assertEquals("Hello World", childBean);
	}

	@Configuration
	@Import(ChildConfig.class)
	public static class MainConfig
	{
		@Bean
		public String mainBean()
		{
			return "Hello";
		}
	}

	@Configuration
	public static class ChildConfig
	{
		@Inject
		private String mainBean;

		@Bean
		public String childBean()
		{
			return mainBean+" World";
		}
	}
}
