package cz.znj.kvr.sw.exp.java.guavaexp.test;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.MapMaker;
import com.google.common.primitives.UnsignedLong;
import com.google.common.primitives.UnsignedLongs;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Created by rat on 2015-09-20.
 */
public class ExpiringCacheTest
{
	public void			safeSleep(long ms)
	{
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void			testExpiry()
	{
		Cache<String, Integer> map = CacheBuilder.newBuilder()
			.expireAfterWrite(500, TimeUnit.MILLISECONDS)
			.<String, Integer>build();

		map.put("hello", 0);
		map.put("world", 1);
		Assert.assertNotNull(map.getIfPresent("hello"));
		Assert.assertNotNull(map.getIfPresent("world"));
		safeSleep(600);
		Assert.assertNull(map.getIfPresent("hello"));
		Assert.assertNull(map.getIfPresent("world"));
	}
}
