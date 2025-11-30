package cz.znj.kvr.sw.exp.java.spring.cache;

import lombok.Getter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.cache.annotation.CacheResult;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;


@ContextConfiguration(classes = CacheTest.MainConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class CacheTest
{
	@Inject
	private TestService testService;

	@Test
	public void testCache()
	{
		AtomicLong callCounter = testService.getCallCounter();
		Assert.assertEquals(0, callCounter.longValue());
		testService.method("Zbynek");
		Assert.assertEquals(1, callCounter.longValue());
		testService.method("Zbynek");
		Assert.assertEquals(1, callCounter.longValue());
		testService.method("Zbynek");
		testService.method("Zbynek");
		testService.method("Zbynek");
		testService.method("Zbynek");
		testService.method("Zbynek");
		testService.method("Zbynek");
		Assert.assertEquals(1, callCounter.longValue());
	}

	public interface TestService
	{
		public AtomicLong getCallCounter();

		String method(String name);
	}

	@Service
	public static class TestServiceImpl implements TestService
	{
		@Getter
		private AtomicLong callCounter = new AtomicLong();

		@CacheResult(cacheName = "method")
		@Cacheable(cacheNames = "method")
		public String method(String name) {
			callCounter.incrementAndGet();
			return "Hello "+name;
		}
	}

	@Configuration
	@EnableCaching
	public static class MainConfig
	{
		@Bean
		public CacheManager cacheManager()
		{
			ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
			cacheManager.setCacheNames(Collections.singleton("method"));
			return cacheManager;
		}

		@Bean
		public TestService testService()
		{
			return new TestServiceImpl();
		}
	}
}
