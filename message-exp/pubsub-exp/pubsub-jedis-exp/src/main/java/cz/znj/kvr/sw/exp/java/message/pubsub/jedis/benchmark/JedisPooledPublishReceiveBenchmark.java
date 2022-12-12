package cz.znj.kvr.sw.exp.java.message.pubsub.jedis.benchmark;

import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.executor.SingleWorkExecutor;
import net.dryuf.concurrent.executor.WorkExecutor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


/**
 * Simple Jedis pub-sub tester.
 *
 * The Jedis client is not synchronized, therefore must have an instance per subscriber.
 */
@Log4j2
public class JedisPooledPublishReceiveBenchmark
{
	private final ExecutorService blockingExecutor = Executors.newCachedThreadPool((runnable) -> {
			Thread t = new Thread(runnable);
			t.setDaemon(true);
			return t;
		});

	private final String url = "redis://localhost:7207";

	private JedisPooled jedisPooled;

	private WorkExecutor<Consumer<JedisPooled>, Void> jedisExecutor;

	public static void main(String[] args) throws Exception
	{
		System.exit(new JedisPooledPublishReceiveBenchmark().run(args));
	}

	public int run(String[] args) throws Exception
	{
		setupJedis();
		try {
			publishTest();
		}
		finally {
			this.jedisExecutor.close();
		}
		return 0;
	}

	public void publishTest() throws Exception
	{
		log.info("publishTest: START");

		AtomicInteger pending = new AtomicInteger(1);

		CompletableFuture<JedisPubSub> listener1 = new CompletableFuture<>();
		CountDownLatch exitLatch = new CountDownLatch(1);

		blockingExecutor.execute(() -> {
				try {
					createListener().subscribe(new JedisPubSub()
						{
							@Override
							public void onSubscribe(String channel, int count)
							{
								log.info("subscribed to: channel={} count={}", channel
									, count);
								listener1.complete(this);
							}

							@Override
							public void onMessage(String channel,
									      String message)
							{
								int value = pending.decrementAndGet();
								if (value == 0 || value == 65536) {
									synchronized (pending) {
										pending.notify();
									}
								}
							}
						},
						"Channel-1");
					log.info("Exiting Channel-1  listener");
				}
				catch (Throwable ex) {
					listener1.completeExceptionally(ex);
				}
				finally {
					exitLatch.countDown();
				}
			});

		listener1.get();

		long count;
		long started = System.currentTimeMillis();
		for (count = 0; ; ++count) {
			pending.incrementAndGet();
			jedisExecutor.submit(j -> j.publish("Channel-1", "Message"));
			if (count%65536 == 0) {
				if (System.currentTimeMillis()-started >= 10_000) {
					break;
				}
				else if (pending.get() > 65536) {
					synchronized (pending) {
						while (pending.get() > 65536) {
							pending.wait();
						}
					}
				}
			}
		}
		if (pending.decrementAndGet() != 0) {
			synchronized (pending) {
				while (pending.get() != 0) {
					pending.wait();
				}
			}
		}
		long end = System.currentTimeMillis();
		log.info("Completed: messages={} time={} ms speed={} msg/s", count, end-started, count*1000L/(end-started));

		try (BenchmarkFormatter formatter = new BenchmarkFormatter(System.out)) {
			formatter.printBenchmark(BenchmarkFormatter.Benchmark.builder()
				.name("Jedis.publishReceive")
				.mode("avgt")
				.units("ops/s")
				.score(String.valueOf(count*1000L/(end-started)))
				.build());
		}

		listener1.get().unsubscribe();

		exitLatch.await();

		log.info("publishTest EXIT");
	}

	public void setupJedis()
	{
		this.jedisPooled = new JedisPooled(url);
		// See https://redis.io/docs/manual/keyspace-notifications/
		//jedisPool.configSet("notify-keyspace-events", "AKExe");
		jedisExecutor = new SingleWorkExecutor<>(
			Executors.newFixedThreadPool(10*Runtime.getRuntime().availableProcessors()),
			(Consumer<JedisPooled> item) -> {
				item.accept(jedisPooled);
				return null;
			}
		);
	}

	public Jedis createListener()
	{
		Jedis jedisListener = new Jedis(url);
		// See https://redis.io/docs/manual/keyspace-notifications/
		jedisListener.configSet("notify-keyspace-events", "AKExe");
		return jedisListener;
	}
}
