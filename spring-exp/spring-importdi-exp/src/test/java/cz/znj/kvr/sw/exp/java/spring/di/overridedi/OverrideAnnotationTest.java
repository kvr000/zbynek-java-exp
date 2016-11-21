package cz.znj.kvr.sw.exp.java.spring.di.overridedi;

import cz.znj.kvr.sw.exp.java.spring.di.common.MyBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;


@ContextConfiguration(classes = {OverrideAnnotationTest.MainConfig.class, OverrideAnnotationTest.OverrideConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class OverrideAnnotationTest
{
	@Inject
	private MyBean myBean;

	@Test
	public void testOverride()
	{
		Assert.assertEquals(1, myBean.getId());
	}

	@Configuration
	public static class MainConfig
	{
		@Bean
		public MyBean myBean()
		{
			return new MyBean(0);
		}
	}

	@Configuration
	public static class OverrideConfig
	{
		@Bean
		public MyBean myBean()
		{
			return new MyBean(1);
		}
	}
}
