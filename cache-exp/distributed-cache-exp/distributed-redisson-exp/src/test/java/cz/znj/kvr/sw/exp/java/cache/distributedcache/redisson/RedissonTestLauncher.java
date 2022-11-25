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
package cz.znj.kvr.sw.exp.java.cache.distributedcache.redisson;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;


@Slf4j
public class RedissonTestLauncher
{
	private RedissonClient client1;
	private RedissonClient client2;

	private RMapCache<Integer, Integer> squareNumberCache1;
	private RMapCache<Integer, Integer> squareNumberCache2;

	private long calculations;

	private static VarHandle CALCULATIONS_HANDLE= new Supplier<>() { @SneakyThrows
	@Override public VarHandle get() {
		return MethodHandles.privateLookupIn(RedissonTestLauncher.class, MethodHandles.lookup())
			.findVarHandle(RedissonTestLauncher.class, "calculations", long.class);
	} }.get();

	private static Map<String, String> TESTS = ImmutableMap.of(
		"propagation", "testPropagation",
		"clear", "testClear",
		"benchmark", "benchmark"
	);


	public RedissonTestLauncher()
	{
		log.info("Hello");
		setupCache();
	}

	public static void main(String[] args) throws Exception
	{
		System.exit(new RedissonTestLauncher().run(args));
	}

	public int run(String[] args) throws Exception
	{
		warmup();

		for (String name: args.length == 0 ? TESTS.keySet() : Arrays.asList(args)) {
			String method = Optional.of(TESTS.get(name))
				.orElseThrow(() -> new UnsupportedOperationException("unknown test: "+name+", supported: "+TESTS.keySet()));
			Stopwatch watch = Stopwatch.createStarted();
			MethodUtils.invokeExactMethod(this, method);
			log.info("Test finished: test={} time={}ms", name, watch.elapsed(TimeUnit.MILLISECONDS));
		}

		return 0;
	}

	public void testPropagation() throws TimeoutException
	{
		squareNumberCache1.put(5, 25);
		waitForValue(squareNumberCache1, 5, 25);
		waitForValue(squareNumberCache2, 5, 25);

		squareNumberCache2.put(5, 1);
		waitForValue(squareNumberCache2, 5, 1);
		waitForValue(squareNumberCache1, 5, 1);
	}

	public void testClear() throws TimeoutException
	{
		squareNumberCache2.put(5, 1);
		waitForValue(squareNumberCache1, 5, 1);
		squareNumberCache1.clear();
		waitForValue(squareNumberCache2, 5, null);
	}

	public void benchmark() throws Exception
	{
		squareNumberCache1.put(5, 25);
		benchmark(squareNumberCache1);
		benchmark(squareNumberCache2);
	}

	private void warmup()
	{
		Stopwatch timer = Stopwatch.createStarted();
		for (int i = 0; i < 10000; ++i) {
			squareNumberCache1.put(5, 0);
			squareNumberCache1.get(5);
			timer.elapsed(TimeUnit.NANOSECONDS);
		}
		log.info("Warmup completed in {} ns", timer.elapsed(TimeUnit.NANOSECONDS));
	}

	private void benchmark(RMapCache<Integer, Integer> cache)
	{
		Stopwatch timer = Stopwatch.createStarted();
		int count;
		for (count = 0; ; ++count) {
			cache.get(5);
			if ((count&1023) == 0) {
				if (timer.elapsed(TimeUnit.SECONDS) > 0)
					break;
			}
		}
		log.info("One lookup in {} ns", timer.elapsed(TimeUnit.NANOSECONDS) / count);
	}

	private void waitForValue(RMapCache<Integer, Integer> cache, Integer key, Integer value) throws TimeoutException
	{
		Stopwatch timer = Stopwatch.createStarted();
		for (long timeout = 1; timeout < 5_000_000_000L; timeout *= 2) {
			Integer result = cache.get(key);
			if (Objects.equals(value, result)) {
				log.info("Received update in: {} ns", timer.elapsed(TimeUnit.NANOSECONDS));
				return;
			}
			log.debug("Stale result in cache: {}", result);
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
		Config config = new Config();
		config
			.useSingleServer()
			.setAddress("redis://localhost:7207");

		this.client1 = Redisson.create(config);
		this.client2 = Redisson.create(config);

		this.squareNumberCache1 = client1.<Integer, Integer>getMapCache("RedissonCache-exp");
		this.squareNumberCache2 = client2.<Integer, Integer>getMapCache("RedissonCache-exp");
	}
}
