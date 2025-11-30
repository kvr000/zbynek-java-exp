package cz.znj.kvr.sw.exp.java.spring.di.inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import jakarta.inject.Inject;
import jakarta.inject.Named;


@ContextConfiguration(classes = { InjectConfigTest.MainConfig.class, InjectConfigTest.SecondConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class InjectConfigTest
{
	@Inject
	private String childBean;

	@Inject
	@Named("injectConfigTest.MainConfig")
	private ConfigParent mainConfig;

	@Inject
	@Named("injectConfigTest.SecondConfig")
	private ConfigParent secondConfig;

	@Inject
	@Named("cz.znj.kvr.sw.exp.java.spring.di.inject.InjectConfigTest$ChildConfig")
	private ConfigParent childConfig;

	@Test
	public void testImport()
	{
		Assert.assertNotNull(childBean);
		Assert.assertEquals("Hello World", childBean);
	}

	public static class ConfigParent
	{
	}

	@Configuration
	@Import(ChildConfig.class)
	public static class MainConfig extends ConfigParent
	{
		@Bean
		public String mainBean()
		{
			return "Hello";
		}
	}

	@Configuration
	@Import(ChildConfig.class)
	public static class SecondConfig extends ConfigParent
	{
		@Bean
		public String secondBean()
		{
			return "World";
		}
	}

	@Configuration
	public static class ChildConfig extends ConfigParent
	{
		@Inject
		private String mainBean;

		@Inject
		private String secondBean;

		@Bean
		public String childBean()
		{
			return mainBean+" "+secondBean;
		}
	}
}
