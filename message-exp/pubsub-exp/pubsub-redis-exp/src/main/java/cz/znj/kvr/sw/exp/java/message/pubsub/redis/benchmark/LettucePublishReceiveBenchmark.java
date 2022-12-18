package cz.znj.kvr.sw.exp.java.message.pubsub.redis.benchmark;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
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
		     LettuceCommon.Subscription subscription = common.createSubscription()
		) {
			AtomicInteger pending = new AtomicInteger();

			RedisPubSubReactiveCommands<String, String> reactive = connection.reactive();
			common.createListener(subscription, "Channel-1", pending).subscribe();

			runBenchmarkLoop(
				pending,
				(message) -> reactive.publish("Channel-1", message).subscribe()
			);
		}
	}
}
