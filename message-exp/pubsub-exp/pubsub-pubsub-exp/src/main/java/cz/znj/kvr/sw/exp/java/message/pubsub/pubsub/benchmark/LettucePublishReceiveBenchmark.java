package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub.benchmark;

import cz.znj.kvr.sw.exp.java.message.pubsub.pubsub.LettuceCommon;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisConnectionStateAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Simple Jedis pub-sub tester.
 *
 * The Jedis client is not synchronized, therefore must have an instance per subscriber.
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class LettucePublishReceiveBenchmark extends AbstractPublishReceiveBenchmark
{
	private final LettuceCommon common;

	@Override
	protected void runBenchmark() throws Exception
	{
		try (StatefulRedisPubSubConnection<String, String> connection = common.getRedisClient().connectPubSub();
		     LettuceCommon.Subscription subscription = common.createSubscription(new RedisConnectionStateAdapter() {
			     @Override
			     public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
			     {
				     log.info("Reconnected subscriber");
			     }
		     })
		) {
			AtomicInteger pending = new AtomicInteger();

			LettuceCommon.Publisher publisher = common.getPublisher(new RedisConnectionStateAdapter() {
				@Override
				public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
				{
					log.info("Reconnected publisher");
				}
			});
			publisher.connectedFuture().get();
			common.createListener(subscription, "Channel-bench", pending).get();

			runBenchmarkLoop(
				pending,
				(message) -> publisher.publish("Channel-bench", message)
			);
		}
	}
}
