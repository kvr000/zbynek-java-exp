package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;

import javax.inject.Inject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Simple Jedis pub-sub tester.
 *
 * The Jedis client is not synchronized, therefore must have an instance per subscriber.
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class JedisPooledPublishReceiveBenchmark extends AbstractPublishReceiveBenchmark
{
	private final JedisCommon common;

	@Override
	protected void runBenchmark() throws Exception
	{
		try (JedisPooled jedisPooled = common.createJedisPooled()) {
			AtomicInteger pending = new AtomicInteger();
			CountDownLatch exitLatch = new CountDownLatch(1);
			JedisPubSub listener = common.createListener("Channel-bench", exitLatch, pending);

			runBenchmarkLoop(
				pending,
				(message) -> jedisPooled.publish("Channel-bench", message)
			);

			listener.unsubscribe();

			exitLatch.await();
		}
	}
}
