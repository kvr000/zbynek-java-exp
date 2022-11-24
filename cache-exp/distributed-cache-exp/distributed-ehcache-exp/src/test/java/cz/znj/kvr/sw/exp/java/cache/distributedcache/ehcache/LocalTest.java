/*
 * Copyright 2015 Zbynek Vyskovsky mailto:kvr000@gmail.com http://github.com/kvr000/zbynek-java/exp/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.znj.kvr.sw.exp.java.cache.distributedcache.ehcache;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.clustered.client.config.builders.ClusteringServiceConfigurationBuilder;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URI;
import java.util.function.UnaryOperator;


public class LocalTest
{
	private CacheManager cacheManager;
	private Cache<Integer, Integer> squareNumberCache;

	private long calculations;

	private static VarHandle CALCULATIONS_HANDLE;

	static {
		try {
			CALCULATIONS_HANDLE = MethodHandles.privateLookupIn(LocalTest.class, MethodHandles.lookup())
				.findVarHandle(LocalTest.class, "calculations", long.class);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public LocalTest()
	{
		setupCache();
	}

	@Test
	public void testDistributedUpdate()
	{
		getSquareValueOfNumber(5);
		getSquareValueOfNumber(5);
		Assert.assertEquals(1, calculations);
		squareNumberCache.remove(5);
		getSquareValueOfNumber(5);
		Assert.assertEquals(2, calculations);
	}

	private int getSquareValueOfNumber(int input) {
		if (squareNumberCache.containsKey(input)) {
			return squareNumberCache.get(input);
		}

		System.out.println("Calculating square value of " + input +
			" and caching result.");
		CALCULATIONS_HANDLE.getAndAdd(this, 1L);

		int squaredValue = input * input;
		squareNumberCache.put(input, squaredValue);

		return squaredValue;
	}


	public void setupCache() {
		cacheManager = CacheManagerBuilder
			.newCacheManagerBuilder()
			.build(true);

		squareNumberCache = cacheManager
			.createCache("squaredNumber", CacheConfigurationBuilder
				.newCacheConfigurationBuilder(
					Integer.class, Integer.class,
					ResourcePoolsBuilder.heap(10)));
	}
}
