package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub;

import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisConnectionStateAdapter;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.SharedScheduledExecutorInstance;
import org.apache.commons.lang3.mutable.MutableObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Simple RabbitMq pub-sub tester.
 *
 * The Jedis client is not synchronized, therefore must have an instance per subscriber.
 */
@Log4j2
public class RabbitMqAmqpPublishReceive
{
	private final String rabbitMqUrl = "localhost:5672";

	public static void main(String[] args) throws Exception
	{
		System.exit(new RabbitMqAmqpPublishReceive().run(args));
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

		try (RabbitMqAmqpCommon rabbitMqCommon = setupRabbitMq()) {

			CountDownLatch latch = new CountDownLatch(3);

			RabbitMqAmqpCommon.Subscription subscription = rabbitMqCommon.createSubscription(new RedisConnectionStateAdapter() {
				@Override
				public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
				{
					log.info("Reconnected subscriber");
				}
			});
			Void subscriptions = subscription.subscribe(
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

			RabbitMqAmqpCommon.Publisher publisher = rabbitMqCommon.getPublisher(new RedisConnectionStateAdapter()
			{
				@Override
				public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
				{
					log.info("Publisher connected");
				}
			});

			log.info("Sending Channel-1 Message-1");
			publisher.publish("Channel-1", "Message-1")
				.handle((v, e) -> { log.info("Message-1-1: {}", v, e); return null; });
			log.info("Sending Channel-2 Message-1");
			publisher.publish("Channel-2", "Message-1")
				.handle((v, e) -> { log.info("Message-2-1: {}", v, e); return null; });
			log.info("Sending Channel-3 Message-1");
			publisher.publish("Channel-3", "Message-1")
				.handle((v, e) -> { log.info("Message-3-1: {}", v, e); return null; });
			log.info("Sending Channel-1 Message-2");
			publisher.publish("Channel-1", "Message-2")
				.handle((v, e) -> { log.info("Message-1-2: {}", v, e); return null; });

			latch.await();
		}

		log.info("publishTest EXIT");
	}

	public void readTest() throws Exception
	{
		log.info("readTest: START");

		try (LettuceCommon lettuceCommon = new LettuceCommon(rabbitMqUrl)) {
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

	public void autoconnectTest() throws Exception
	{
		log.info("autoconnectTest: START");

		try (RabbitMqReactorCommon common = new RabbitMqReactorCommon(rabbitMqUrl, true)) {
			MutableObject<RabbitMqReactorCommon.Publisher> publisherRef = new MutableObject<>();

			RabbitMqReactorCommon.Subscription subscription = common.createSubscription(new RedisConnectionStateAdapter() {
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
					SharedScheduledExecutorInstance.getScheduledExecutorService().schedule(() ->
							Optional.ofNullable(publisherRef.getValue()).ifPresent(p -> p.publish("Channel-read", "Delayed Message")),
						1,
						TimeUnit.SECONDS
					);
					log.info("Reconnected Channel-read");
				}
			).join();

			RabbitMqReactorCommon.Publisher publisher = common.getPublisher(new RedisConnectionStateAdapter() {
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

	public RabbitMqAmqpCommon setupRabbitMq()
	{
		return new RabbitMqAmqpCommon(rabbitMqUrl);
	}

	public RabbitMqReactorCommon setupRabbitMqAutoConnect()
	{
		return new RabbitMqReactorCommon(rabbitMqUrl, true);
	}
}
