package cz.znj.kvr.sw.exp.java.message.pubsub.redis.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;


@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Singleton
public class JedisCommon
{
	private final ExecutorService blockingExecutor;

	private final RedisBenchmarkRunner.Options options;

	public Jedis createJedis()
	{
		return new Jedis(options.redisUrl);
	}

	public JedisPool createJedisPool()
	{
		return new JedisPool(options.redisUrl);
	}

	public JedisPooled createJedisPooled()
	{
		return new JedisPooled(options.redisUrl);
	}

	public JedisPubSub createListener(String channel, CountDownLatch exitLatch, AtomicInteger pending)
	{
		CompletableFuture<JedisPubSub> future = new CompletableFuture<>();
		blockingExecutor.execute(() -> {
			try (Jedis jedis = createJedis()) {
				jedis.subscribe(new JedisPubSub()
						{
							@Override
							public void onSubscribe(String channel, int count)
							{
								log.info("subscribed to: " +
										"channel={} count={}", channel
									, count);
								future.complete(this);
							}

							@Override
							public void onMessage(String channel,
									      String message)
							{
								if (pending != null) {
									int value = pending.decrementAndGet();
									if (value == 0 || value == 65536) {
										synchronized (pending) {
											pending.notify();
										}
									}
								}
							}
						},
					channel);
				log.info("Exiting {} listener", channel);
			}
			catch (Throwable ex) {
				future.completeExceptionally(ex);
			}
			finally {
				exitLatch.countDown();
			}
		});
		try {
			return future.get();
		}
		catch (InterruptedException|ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
