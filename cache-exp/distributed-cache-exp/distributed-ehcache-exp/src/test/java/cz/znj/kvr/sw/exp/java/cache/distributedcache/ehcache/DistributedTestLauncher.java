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

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.clustered.client.config.builders.ClusteredResourcePoolBuilder;
import org.ehcache.clustered.client.config.builders.ClusteredStoreConfigurationBuilder;
import org.ehcache.clustered.client.config.builders.ClusteringServiceConfigurationBuilder;
import org.ehcache.clustered.common.Consistency;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.testng.Assert;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
public class DistributedTestLauncher
{
	private CacheManager cacheManager1;
	private CacheManager cacheManager2;
	private Cache<Integer, Integer> squareNumberCache1;
	private Cache<Integer, Integer> squareNumberCache2;

	private long calculations;

	private static VarHandle CALCULATIONS_HANDLE;

	static {
		try {
			CALCULATIONS_HANDLE = MethodHandles.privateLookupIn(DistributedTestLauncher.class, MethodHandles.lookup())
				.findVarHandle(DistributedTestLauncher.class, "calculations", long.class);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public DistributedTestLauncher()
	{
		log.info("Hello");
		setupCache();
	}

	public static void main(String[] args) throws Exception
	{
		System.exit(new DistributedTestLauncher().run(args));
	}

	public int run(String[] args) throws Exception
	{
		squareNumberCache1.put(5, 25);
		waitForValue(squareNumberCache2, 5, 25);

		squareNumberCache2.put(5, 1);
		waitForValue(squareNumberCache1, 5, 1);

		return 0;
	}

	private void waitForValue(Cache<Integer, Integer> cache, Integer key, Integer value) throws TimeoutException
	{
		Stopwatch timer = Stopwatch.createStarted();
		for (long timeout = 1; timeout < 5_000_000_000L; timeout *= 2) {
			Integer result = cache.get(key);
			if (value.equals(result)) {
				log.info("Received update in: {} ns", timer.elapsed(TimeUnit.NANOSECONDS));
				return;
			}
			log.info("Stale result in cache1: {}", result);
			try {
				TimeUnit.NANOSECONDS.sleep(timeout);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		throw new TimeoutException("Failed to propagate cache within 10000 ms");
	}

	public void setupCache() {
		cacheManager1 = CacheManagerBuilder
			.newCacheManagerBuilder()
			.with(ClusteringServiceConfigurationBuilder.cluster(URI.create("terracotta://localhost/distributed-ehcache-exp"))
				.autoCreate()
			)
			.build(true);

		squareNumberCache1 = cacheManager1
			.createCache("squaredNumber", CacheConfigurationBuilder
				.newCacheConfigurationBuilder(
					Integer.class, Integer.class,
					ResourcePoolsBuilder.heap(10)
						.with(ClusteredResourcePoolBuilder.clusteredDedicated("main", 2, MemoryUnit.MB))
				)
				.withService(ClusteredStoreConfigurationBuilder.withConsistency(Consistency.STRONG))
			);

		cacheManager2 = CacheManagerBuilder
			.newCacheManagerBuilder()
			.with(ClusteringServiceConfigurationBuilder.cluster(URI.create("terracotta://localhost/distributed-ehcache-exp"))
				.autoCreate()
			)
			.build(true);

		squareNumberCache2 = cacheManager2
			.createCache("squaredNumber", CacheConfigurationBuilder
				.newCacheConfigurationBuilder(
					Integer.class, Integer.class,
					ResourcePoolsBuilder.heap(10)
						.with(ClusteredResourcePoolBuilder.clusteredDedicated("main", 2, MemoryUnit.MB))
				)
				.withService(ClusteredStoreConfigurationBuilder.withConsistency(Consistency.STRONG))
			);
	}
}
