package cz.znj.kvr.sw.exp.java.spring.di.overridedi;

import cz.znj.kvr.sw.exp.java.spring.di.common.MyBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;


@ContextConfiguration(classes = {OverrideImportTest.MainConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class OverrideImportTest
{
	@Inject
	private MyBean myBean;

	@Test
	public void testOverride()
	{
		Assert.assertEquals(0, myBean.getId());
	}

	@Configuration
	@Import(ImportedConfig.class)
	public static class MainConfig
	{
		@Bean
		public MyBean myBean()
		{
			return new MyBean(0);
		}
	}

	@Configuration
	public static class ImportedConfig
	{
		@Bean
		public MyBean myBean()
		{
			return new MyBean(1);
		}
	}
}
