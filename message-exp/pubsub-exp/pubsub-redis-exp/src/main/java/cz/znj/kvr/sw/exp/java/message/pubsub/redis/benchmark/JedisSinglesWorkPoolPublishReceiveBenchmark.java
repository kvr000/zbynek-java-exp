package cz.znj.kvr.sw.exp.java.message.pubsub.redis.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.executor.ClosingExecutor;
import net.dryuf.concurrent.executor.SingleWorkExecutor;
import net.dryuf.concurrent.executor.WorkExecutor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import javax.inject.Inject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


/**
 * Simple Jedis pub-sub tester.
 *
 * The Jedis client is not synchronized, therefore must have an instance per subscriber.
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class JedisSinglesWorkPoolPublishReceiveBenchmark extends AbstractPublishReceiveBenchmark
{
	private final JedisCommon common;

	@Override
	protected void runBenchmark() throws Exception
	{
		try (WorkExecutor<Consumer<Jedis>, Void> jedisExecutor = setupJedis()) {
			AtomicInteger pending = new AtomicInteger(0);

			CountDownLatch exitLatch = new CountDownLatch(1);

			JedisPubSub listener = common.createListener("Channel-1", exitLatch, pending);

			runBenchmarkLoop(
				pending,
				(message) -> jedisExecutor.submit(j -> j.publish("Channel-1", message))
			);

			listener.unsubscribe();

			exitLatch.await();

			log.info("publishTest EXIT");
		}
	}

	public WorkExecutor<Consumer<Jedis>, Void> setupJedis()
	{
		JedisPool jedisPool = common.createJedisPool();
		return new SingleWorkExecutor<>(
			new ClosingExecutor(Executors.newFixedThreadPool(10*Runtime.getRuntime().availableProcessors()), jedisPool),
			(Consumer<Jedis> item) -> {
				try (Jedis jedis = jedisPool.getResource()) {
					item.accept(jedis);
				}
				return null;
			}
		);
	}
}
