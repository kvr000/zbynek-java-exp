package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.base.concurrent.executor.BatchWorkExecutor;
import net.dryuf.base.concurrent.executor.ClosingExecutor;
import net.dryuf.base.concurrent.executor.WorkExecutor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Simple Jedis pub-sub tester.
 *
 * The Jedis client is not synchronized, therefore must have an instance per subscriber.
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class JedisWorkPoolPublishReceiveBenchmark extends AbstractPublishReceiveBenchmark
{
	private final JedisCommon common;

	@Override
	public void runBenchmark() throws Exception
	{
		try (WorkExecutor<Consumer<Jedis>, Void> jedisExecutor = setupJedis()) {
			AtomicInteger pending = new AtomicInteger();
			CountDownLatch exitLatch = new CountDownLatch(1);
			JedisPubSub listener = common.createListener("Channel-bench", exitLatch, pending);

			runBenchmarkLoop(
				pending,
				(message) -> jedisExecutor.submit(j -> j.publish("Channel-bench", message))
			);

			listener.unsubscribe();

			exitLatch.await();
		}
	}

	public WorkExecutor<Consumer<Jedis>, Void> setupJedis()
	{
		JedisPool jedisPool = common.createJedisPool();
		return new BatchWorkExecutor<>(
			new ClosingExecutor(Executors.newFixedThreadPool(10*Runtime.getRuntime().availableProcessors()), jedisPool),
			100,
			(List<Consumer<Jedis>> items) -> {
				try (Jedis jedis = jedisPool.getResource()) {
					//log.info("Processing messages: count={}", items.size());
					return items.stream()
						.map(item -> {
							item.accept(jedis);
							return CompletableFuture.completedFuture((Void) null);
						})
						.collect(Collectors.toList());
				}
			}
		);
	}
}
