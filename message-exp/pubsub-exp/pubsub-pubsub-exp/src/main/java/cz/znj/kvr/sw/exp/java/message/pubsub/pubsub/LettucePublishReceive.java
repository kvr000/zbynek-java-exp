package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub;

import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisConnectionStateAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Simple Jedis pub-sub tester.
 *
 * The Jedis client is not synchronized, therefore must have an instance per subscriber.
 */
@Log4j2
public class LettucePublishReceive
{
	private final String redisUrl = "redis://localhost:7207";

	public static void main(String[] args) throws Exception
	{
		System.exit(new LettucePublishReceive().run(args));
	}

	public int run(String[] args) throws Exception
	{
		switch (Optional.ofNullable(args.length > 0 ? args[0] : "").orElse("")) {
		case "":
			publishTest();
			break;

		case "read":
			readTest();
			break;

		case "autoreconnect":
			autoconnectTest();
			break;
		}

		return 0;
	}

	public void publishTest() throws Exception
	{
		log.info("publishTest: START");

		try (LettuceCommon lettuceCommon = setupLettuce()) {

			CountDownLatch latch = new CountDownLatch(3);

			LettuceCommon.Subscription subscription = lettuceCommon.createSubscription(new RedisConnectionStateAdapter() {
				@Override
				public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
				{
					log.info("Reconnected subscriber");
				}
			});
			subscription.subscribe(
					"Channel-1",
					(message) -> {
						log.info("Received Channel-1: "+message);
						latch.countDown();
					},
					() -> {}
				).runAfterBoth(
					subscription.subscribe(
						"Channel-2",
						(message) -> {
							log.info("Received Channel-2: "+message);
							latch.countDown();
						},
						() -> {}
					),
					() -> {}
				)
				.get();

			StatefulRedisPubSubConnection<String, String> publisher = lettuceCommon.getPubSub().get();
			publisher.addListener(new RedisConnectionStateAdapter()
			{
				@Override
				public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
				{
					log.info("Publisher connected");
				}
			});

			log.info("Sending Channel-1 Message-1");
			publisher.async().publish("Channel-1", "Message-1");
			log.info("Sending Channel-2 Message-1");
			publisher.async().publish("Channel-2", "Message-1");
			log.info("Sending Channel-3 Message-1");
			publisher.async().publish("Channel-3", "Message-1");
			log.info("Sending Channel-1 Message-2");
			publisher.async().publish("Channel-1", "Message-2");

			latch.await();
		}

		log.info("publishTest EXIT");
	}

	public void readTest() throws Exception
	{
		log.info("readTest: START");

		try (LettuceCommon lettuceCommon = new LettuceCommon(redisUrl)) {
			LettuceCommon.Subscription subscription = lettuceCommon.createSubscription(new RedisConnectionStateAdapter() {
				@Override
				public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
				{
					log.info("Reconnected subscriber");
				}
			});
			subscription.subscribe(
				"Channel-read",
				(message) -> {
					log.info("Received Channel-read: {}", message);
				},
				() -> {
					log.info("Reconnected Channel-read");
				}
			).join();

			LettuceCommon.Publisher publisher = lettuceCommon.getPublisher(new RedisConnectionStateAdapter() {
				@Override
				public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
				{
					log.info("Reconnected subscriber");
				}
			});

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
				for (; ; ) {
					String line = reader.readLine();
					if (line == null) {
						break;
					}
					publisher.publish("Channel-read", line).handle((v, x) -> {
						System.out.println("publish: "+v+" "+x);
						return v;
					});
				}
			}
		}

		log.info("readTest EXIT");
	}

	/**
	 * Test with autoreconnect option.
	 *
	 * It works but connect notification happens before the subscribe is submitted, therefore there may be messages
	 * lost.
	 *
	 * @throws Exception
	 */
	public void autoconnectTest() throws Exception
	{
		log.info("autoconnectTest: START");

		try (LettuceCommon lettuceCommon = new LettuceCommon(redisUrl, true)) {
			MutableObject<LettuceCommon.Publisher> publisherRef = new MutableObject<>();

			LettuceCommon.Subscription subscription = lettuceCommon.createSubscription(new RedisConnectionStateAdapter() {
				@Override
				public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
				{
					log.info("Reconnected subscriber");
				}
			});
			subscription.subscribe(
				"Channel-read",
				(message) -> {
					log.info("Received Channel-read: {}", message);
				},
				() -> {
					Optional.ofNullable(publisherRef.getValue()).ifPresent(p -> p.publish("Channel-read", "Welcome Message"));
					lettuceCommon.getRedisClient().getResources().eventExecutorGroup().schedule(() ->
							Optional.ofNullable(publisherRef.getValue()).ifPresent(p -> p.publish("Channel-read", "Delayed Message")),
						1,
						TimeUnit.SECONDS
					);
					log.info("Reconnected Channel-read");
				}
			).join();

			LettuceCommon.Publisher publisher = lettuceCommon.getPublisher(new RedisConnectionStateAdapter() {
				@Override
				public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
				{
					log.info("Reconnected publisher");
				}
			});
			publisherRef.setValue(publisher);

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
				for (; ; ) {
					String line = reader.readLine();
					if (line == null) {
						break;
					}
					publisher.publish("Channel-read", line).handle((v, x) -> {
						System.out.println("publish: "+v+" "+x);
						return v;
					});
				}
			}
		}

		log.info("autoconnectTest EXIT");
	}

	public LettuceCommon setupLettuce()
	{
		return new LettuceCommon(redisUrl);
	}

	public LettuceCommon setupLettuceAutoConnect()
	{
		return new LettuceCommon(redisUrl, true);
	}
}
