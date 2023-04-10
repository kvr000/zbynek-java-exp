package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionStateAdapter;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.ChannelMessage;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import lombok.extern.log4j.Log4j2;
import net.dryuf.base.concurrent.future.FutureUtil;
import net.dryuf.base.concurrent.future.ScheduledUtil;
import net.dryuf.base.concurrent.sync.RunSingle;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


@Log4j2
@Singleton
public class LettuceCommon implements AutoCloseable
{
	@ServerUrlInject
	private final String redisUrl;

	private RedisClient redisClient;

	private final boolean autoReconnect;

	@Inject
	public LettuceCommon(@ServerUrlInject String redisUrl)
	{
		this(redisUrl, false);
	}

	public LettuceCommon(@ServerUrlInject String redisUrl, boolean autoReconnect)
	{
		this.redisUrl = redisUrl;
		this.autoReconnect = autoReconnect;
	}

	public synchronized RedisClient getRedisClient()
	{
		if (redisClient == null) {
			redisClient = RedisClient.create(redisUrl);
			redisClient.setOptions(ClientOptions.builder().autoReconnect(autoReconnect).build());
		}
		return redisClient;
	}

	public CompletableFuture<StatefulRedisPubSubConnection<String, String>> getPubSub()
	{
		return ScheduledUtil.scheduleWithFixedDelayUntilComposedSuccess(
			getRedisClient().getResources().eventExecutorGroup(),
			() -> getRedisClient().connectPubSubAsync(new StringCodec(), RedisURI.create(redisUrl)),
			1,
			TimeUnit.SECONDS
		);
	}

	public Subscription createSubscription(RedisConnectionStateListener listener)
	{
		return new Subscription(listener);
	}

	public Publisher getPublisher(RedisConnectionStateListener listener)
	{
		return new Publisher(listener);
	}

	public CompletableFuture<Void> createListener(Subscription subscription, String channel, AtomicInteger pending)
	{
		return subscription.subscribe(
			channel,
			(message) ->

			{
				if (pending != null) {
					int value = pending.decrementAndGet();
					if (value == 0 || value == 65536)
						synchronized (pending) {
							pending.notify();
						}
				}
			},
			() -> {}
		);
	}

	@Override
	public void close()
	{
		if (redisClient != null)
			redisClient.close();
	}

	public class Publisher implements AutoCloseable
	{
		private final RedisConnectionStateListener lifecycleListener;

		private StatefulRedisPubSubConnection<String, String> connection;

		private CompletableFuture<Void> reconnectFuture;

		public Publisher(RedisConnectionStateListener listener)
		{
			this.lifecycleListener = listener;
			reconnect();
		}

		public CompletableFuture<Void> connectedFuture()
		{
			return reconnectFuture;
		}

		public CompletableFuture<Void> publish(String channel, String message)
		{
			if (connection != null) {
				return connection.async().publish(channel, message).thenRun(() -> {}).toCompletableFuture();
			}
			else {
				return CompletableFuture.failedFuture(new IOException("Not connected"));
			}
		}

		private synchronized void reconnect()
		{
			this.connection = null;
			reconnectFuture = getPubSub()
				.thenAccept(connection0 -> {
					connection = connection0;
					if (!connection.getOptions().isAutoReconnect()) {
						connection.addListener(new RedisConnectionStateAdapter() {
							@Override
							public void onRedisDisconnected(RedisChannelHandler<?, ?> connection)
							{
								reconnect();
							}
						});
					}
					connection.addListener(lifecycleListener);
				});
		}

		@Override
		public void close()
		{
			if (connection != null) {
				connection.close();
			}
			reconnectFuture.cancel(true);
		}
	}

	public class Subscription implements AutoCloseable
	{
		private final RedisConnectionStateListener lifecycleListener;

		private StatefulRedisPubSubConnection<String, String> connection;

		private RedisPubSubReactiveCommands<String, String> reactive;

		private CompletableFuture<Void> reconnectFuture;

		/**
		 * Channels to ( subscribe-completion, [ ( message-callback, reconnect-callback ) ] )
		 */
		private final ConcurrentHashMap<String, MutablePair<CompletableFuture<Void>, List<Pair<Consumer<String>, Runnable>>>> subscriptions =
			new ConcurrentHashMap<>();

		public Subscription(RedisConnectionStateListener listener)
		{
			this.lifecycleListener = listener;
			reconnect();
		}

		public synchronized CompletableFuture<Void> subscribe(String channel, Consumer<String> onMessage, Runnable onReconnected)
		{
			MutablePair<CompletableFuture<Void>, List<Pair<Consumer<String>, Runnable>>> result = subscriptions.computeIfAbsent(channel,
				key -> MutablePair.of(reactive == null ? new CompletableFuture<Void>() : subscribeConnected(key), new ArrayList<>()));
			if (result.getLeft().isDone() && connection == null) {
				result.setLeft(new CompletableFuture<>());
			}
			result.getRight().add(Pair.of(onMessage, onReconnected));
			return result.getLeft();
		}

		@Override
		public void close()
		{
			try {
				if (connection != null) {
					connection.close();
				}
			}
			finally {
				reconnectFuture.cancel(true);
			}
		}

		private synchronized void reconnect()
		{
			this.connection = null;
			reconnectFuture = getPubSub()
				.thenAccept(connection0 -> {
					this.connection = connection0;
					this.reactive = connection.reactive();
					this.reactive.observeChannels().subscribe((ChannelMessage<String, String> message) ->
						Optional.ofNullable(subscriptions.get(message.getChannel()))
							.ifPresent(consumers -> consumers.getRight()
								.forEach(consumer -> consumer.getLeft().accept(message.getMessage())))
					);
					reconnectFuture.complete(null);
					for (Map.Entry<String, MutablePair<CompletableFuture<Void>, List<Pair<Consumer<String>, Runnable>>>> subscription: subscriptions.entrySet()) {
						subscribeConnected(subscription.getKey())
							.thenRun(() -> {
								subscription.getValue().getLeft().complete(null);
								subscription.getValue().getRight().forEach(e -> FutureUtil.submitDirect(() -> {
									e.getRight().run();
									return null;
								}));
							});
					}
					connection.addListener(lifecycleListener);
					if (!connection.getOptions().isAutoReconnect()) {
						connection.addListener(new RedisConnectionStateAdapter()
						{
							@Override
							public void onRedisDisconnected(RedisChannelHandler<?, ?> connection)
							{
								reconnect();
							}
						});
					}
					else {
						connection.addListener(new RedisConnectionStateAdapter() {
							@Override
							public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
							{
								synchronized (Subscription.this) {
									for (Map.Entry<String, MutablePair<CompletableFuture<Void>, List<Pair<Consumer<String>, Runnable>>>> subscription : subscriptions.entrySet()) {
										subscription.getValue().getLeft().complete(null);
										subscription.getValue().getRight().forEach(e -> FutureUtil.submitDirect(() -> {
											e.getRight().run();
											return null;
										}));
									}
								}
							}
						});
					}
				});
		}

		private CompletableFuture<Void> subscribeConnected(String channel)
		{
			return connection.async().subscribe(channel).toCompletableFuture();
		}
	}
}
