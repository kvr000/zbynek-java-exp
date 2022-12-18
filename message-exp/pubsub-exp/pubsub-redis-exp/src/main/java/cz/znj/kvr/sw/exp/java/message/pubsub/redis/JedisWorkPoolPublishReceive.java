package cz.znj.kvr.sw.exp.java.message.pubsub.redis;

import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.executor.BatchWorkExecutor;
import net.dryuf.concurrent.executor.ClosingExecutor;
import net.dryuf.concurrent.executor.WorkExecutor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Simple Jedis pub-sub tester.
 *
 * The Jedis client is not synchronized, therefore must have an instance per subscriber.
 */
@Log4j2
public class JedisWorkPoolPublishReceive
{
	private final ExecutorService blockingExecutor = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r);
		t.setDaemon(true);
		return t;
	});

	private final String url = "redis://localhost:7207";

	private JedisPool jedisPool;

	private WorkExecutor<Consumer<Jedis>, Void> jedisExecutor;

	public static void main(String[] args) throws Exception
	{
		System.exit(new JedisWorkPoolPublishReceive().run(args));
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

		CountDownLatch messageLatch = new CountDownLatch(4);
		CountDownLatch exitLatch = new CountDownLatch(2);

		CompletableFuture<JedisPubSub> listener1 = new CompletableFuture<>();
		CompletableFuture<JedisPubSub> listener23 = new CompletableFuture<>();

		blockingExecutor.execute(() -> {
				try {
					Thread.sleep(200);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
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
								log.info("Channel-1   listener: Received "+
										"message: channel={} message={}",
									channel, message);
								messageLatch.countDown();
							}
						},
						"Channel-1");
					log.info("Exiting Channel-1  listener");
					exitLatch.countDown();
				}
				catch (Throwable ex) {
					listener1.completeExceptionally(ex);
				}
			});
		blockingExecutor.execute(() -> {
			try {
				createListener().subscribe(new JedisPubSub()
				{
					@Override
					public void onSubscribe(String channel, int count)
					{
						log.info("subscribed to: channel={} count={}", channel, count);
						if (count == 2)
							listener23.complete(this);
					}

					@Override
					public void onMessage(String channel, String message)
					{
						log.info("Channel-2-3 listener: Received message: channel={} message={}", channel, message);
						messageLatch.countDown();
					}
				}, "Channel-2", "Channel-3");
				log.info("Exiting Channel-23 listener");
				exitLatch.countDown();
			}
			catch (Throwable ex) {
				listener23.completeExceptionally(ex);
			}
		});

		listener1.get();
		listener23.get();

		log.info("Sending Channel-1 Message-1");
		jedisExecutor.submit(j -> j.publish("Channel-1", "Message-1"));
		log.info("Sending Channel-2 Message-1");
		jedisExecutor.submit(j -> j.publish("Channel-2", "Message-1"));
		log.info("Sending Channel-3 Message-1");
		jedisExecutor.submit(j -> j.publish("Channel-3", "Message-1"));
		log.info("Sending Channel-1 Message-2");
		jedisExecutor.submit(j -> j.publish("Channel-1", "Message-2"));
		for (int i = 0; i < 2*Runtime.getRuntime().availableProcessors(); ++i) {
			jedisExecutor.submit(j -> j.publish("Channel-9", "Message-X"));
		}

		messageLatch.await();

		listener1.get().unsubscribe();
		listener23.get().unsubscribe();

		exitLatch.await();

		log.info("publishTest EXIT");
	}

	public void setupJedis()
	{
		this.jedisPool = new JedisPool(url);
		// See https://redis.io/docs/manual/keyspace-notifications/
		//jedisPool.configSet("notify-keyspace-events", "AKExe");
		jedisExecutor = new BatchWorkExecutor<>(
			new ClosingExecutor(Executors.newCachedThreadPool()),
			5,
			(List<Consumer<Jedis>> items) -> {
				try (Jedis jedis = jedisPool.getResource()) {
					log.info("Processing messages: count={}", items.size());
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

	public Jedis createListener()
	{
		Jedis jedisListener = new Jedis(url);
		// See https://redis.io/docs/manual/keyspace-notifications/
		jedisListener.configSet("notify-keyspace-events", "AKExe");
		return jedisListener;
	}
}
