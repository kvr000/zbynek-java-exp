package cz.znj.kvr.sw.exp.java.message.pubsub.redis.benchmark;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.ChannelMessage;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class LettuceCommon
{
	private final RedisBenchmarkRunner.Options options;

	private RedisClient redisClient;

	public synchronized RedisClient getRedisClient()
	{
		if (redisClient == null) {
			redisClient = RedisClient.create(options.redisUrl);
		}
		return redisClient;
	}

	public Subscription createSubscription()
	{
		return new Subscription(getRedisClient().connectPubSub());
	}

	public Mono<Void> createListener(Subscription subscription, String channel, AtomicInteger pending)
	{
		return subscription.subscribe(channel, (message) -> {
			if (pending != null) {
				int value = pending.decrementAndGet();
				if (value == 0 || value == 65536) {
					synchronized (pending) {
						pending.notify();
					}
				}
			}
		});
	}

	public static class Subscription implements AutoCloseable
	{
		private final StatefulRedisPubSubConnection<String, String> connection;

		private final RedisPubSubReactiveCommands<String, String> reactive;

		private final ConcurrentHashMap<String, Pair<Mono<Void>, List<Consumer<String>>>> subscriptions =
			new ConcurrentHashMap<>();

		public Subscription(StatefulRedisPubSubConnection<String, String> connection)
		{
			this.connection = connection;
			this.reactive = connection.reactive();
			this.reactive.observeChannels().subscribe((ChannelMessage<String, String> message) ->
				Optional.ofNullable(subscriptions.get(message.getChannel()))
					.ifPresent(consumers -> consumers.getRight()
						.forEach(consumer -> consumer.accept(message.getMessage())))
			);
		}

		public Mono<Void> subscribe(String channel, Consumer<String> onMessage)
		{
			Pair<Mono<Void>, List<Consumer<String>>> result = subscriptions.computeIfAbsent(channel,
				key -> Pair.of(reactive.subscribe(key), new ArrayList<>()));
			result.getRight().add(onMessage);
			return result.getLeft();
		}

		@Override
		public void close()
		{
			connection.close();
		}
	}
}
