package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.lettuce.core.RedisConnectionStateListener;
import lombok.extern.log4j.Log4j2;
import net.dryuf.base.concurrent.future.ScheduledUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import reactor.rabbitmq.Receiver;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


@Log4j2
@Singleton
public class RabbitMqAmqpCommon implements AutoCloseable
{
	@ServerUrlInject
	private final String serverUrl;

	private ConnectionFactory factory;

	private Receiver receiverClient;

	private final boolean autoReconnect;

	@Inject
	public RabbitMqAmqpCommon(@ServerUrlInject String serverUrl)
	{
		this(serverUrl, false);
	}

	public RabbitMqAmqpCommon(@ServerUrlInject String serverUrl, boolean autoReconnect)
	{
		this.serverUrl = serverUrl;
		this.autoReconnect = autoReconnect;
	}

	public synchronized ConnectionFactory getFactory()
	{
		if (factory == null) {
			factory = new ConnectionFactory();
			try {
				factory.setUri(URI.create("amqp://" + serverUrl));
			}
			catch (URISyntaxException|NoSuchAlgorithmException|KeyManagementException e) {
				throw new RuntimeException(e);
			}
		}
		return factory;
	}

	public CompletableFuture<Connection> getConnection()
	{
		MutableObject<Future<?>> scheduled = new MutableObject<>();
		CompletableFuture<Connection> future = new CompletableFuture<>() {
			@Override
			public boolean cancel(boolean interrupt)
			{
				scheduled.getValue().cancel(true);
				return super.cancel(interrupt);
			}
		};
		synchronized (scheduled) {
			scheduled.setValue(ScheduledUtil.sharedExecutor().schedule(
				() -> {
					try {
						future.complete(getFactory().newConnection());
					}
					catch (IOException|TimeoutException e) {
						future.completeExceptionally(e);
					}
				},
				0,
				TimeUnit.SECONDS
			));
		}
		return future;
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
			(message) -> {
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
	}

	public class Publisher implements AutoCloseable
	{
		private final RedisConnectionStateListener lifecycleListener;

		private Connection connection;

		private Channel channel;

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
			if (this.channel != null) {
				try {
					this.channel.basicPublish(channel, "",
						new AMQP.BasicProperties.Builder()
							.deliveryMode(1)
							.build(),
						message.getBytes(StandardCharsets.UTF_8));
					return CompletableFuture.completedFuture(null);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			else {
				return CompletableFuture.failedFuture(new IOException("Not connected"));
			}
		}

		private synchronized void reconnect()
		{
			// TODO: super simplified and buggy
			this.reconnectFuture = new CompletableFuture<>();
			this.connection = null;
			this.channel = null;
			getConnection().thenAccept(c -> {
				this.connection = c;
				try {
					this.channel = connection.createChannel();
					this.reconnectFuture.complete(null);
//					this.channel.addShutdownListener(new DefaultConsumer(this.channel) {
//						@Override
//						public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig)
//						{
//							lifecycleListener.onRedisDisconnected(null);
//						}
//					});
				}
				catch (IOException e) {
					this.reconnectFuture.completeExceptionally(e);
				}
			});
//			this.connection = null;
//			reconnectFuture = getPubSub()
//				.thenAccept(connection0 -> {
//					connection = connection0;
//					if (!connection.getOptions().isAutoReconnect()) {
//						connection.addListener(new RedisConnectionStateAdapter() {
//							@Override
//							public void onRedisDisconnected(RedisChannelHandler<?, ?> connection)
//							{
//								reconnect();
//							}
//						});
//					}
//					connection.addListener(lifecycleListener);
//				});
		}

		@Override
		public void close()
		{
			if (connection != null) {
				IOUtils.closeQuietly(connection);
			}
			reconnectFuture.cancel(true);
		}
	}

	public class Subscription implements AutoCloseable
	{
		private final RedisConnectionStateListener lifecycleListener;

		private Connection connection;

		private Channel channel;

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
			return reconnectFuture.thenRun(() -> {
				try {
					Subscription.this.channel.exchangeDeclare(channel, "topic");
					AMQP.Queue.DeclareOk declare = Subscription.this.channel.queueDeclare();
					Subscription.this.channel.queueBind(declare.getQueue(), channel, "");
					Subscription.this.channel.basicConsume(declare.getQueue(),
						new DefaultConsumer(Subscription.this.channel)
					{
						@Override
						public void handleDelivery(String consumerTag,
									   Envelope envelope,
									   AMQP.BasicProperties properties,
									   byte[] body)
						{
							onMessage.accept(new String(body, StandardCharsets.UTF_8));
						}
					});
					return;
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}

		@Override
		public void close()
		{
			try {
				IOUtils.closeQuietly(connection);
			}
			finally {
				reconnectFuture.cancel(true);
			}
		}

		private synchronized void reconnect()
		{
			// TODO: super simplified and buggy
			this.reconnectFuture = new CompletableFuture<>();
			this.connection = null;
			this.channel = null;
			getConnection().thenAccept(c -> {
				this.connection = c;
				try {
					this.channel = connection.createChannel();
					this.reconnectFuture.complete(null);
//					this.channel.addShutdownListener(new DefaultConsumer(this.channel) {
//						@Override
//						public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig)
//						{
//							lifecycleListener.onRedisDisconnected(null);
//						}
//					});
				}
				catch (IOException e) {
					this.reconnectFuture.completeExceptionally(e);
				}
			});
//			this.connection = null;
//			reconnectFuture = getPubSub()
//				.thenAccept(connection0 -> {
//					this.connection = connection0;
//					this.reactive = connection.reactive();
//					this.reactive.observeChannels().subscribe((ChannelMessage<String, String> message) ->
//						Optional.ofNullable(subscriptions.get(message.getChannel()))
//							.ifPresent(consumers -> consumers.getRight()
//								.forEach(consumer -> consumer.getLeft().accept(message.getMessage())))
//					);
//					reconnectFuture.complete(null);
//					for (Map.Entry<String, MutablePair<CompletableFuture<Void>, List<Pair<Consumer<String>, Runnable>>>> subscription: subscriptions.entrySet()) {
//						subscribeConnected(subscription.getKey())
//							.thenRun(() -> {
//								subscription.getValue().getLeft().complete(null);
//								subscription.getValue().getRight().forEach(e -> FutureUtil.submitDirect(() -> {
//									e.getRight().run();
//									return null;
//								}));
//							});
//					}
//					connection.addListener(lifecycleListener);
//					if (!connection.getOptions().isAutoReconnect()) {
//						connection.addListener(new RedisConnectionStateAdapter()
//						{
//							@Override
//							public void onRedisDisconnected(RedisChannelHandler<?, ?> connection)
//							{
//								reconnect();
//							}
//						});
//					}
//					else {
//						connection.addListener(new RedisConnectionStateAdapter() {
//							@Override
//							public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
//							{
//								synchronized (Subscription.this) {
//									for (Map.Entry<String, MutablePair<CompletableFuture<Void>, List<Pair<Consumer<String>, Runnable>>>> subscription : subscriptions.entrySet()) {
//										subscription.getValue().getLeft().complete(null);
//										subscription.getValue().getRight().forEach(e -> FutureUtil.submitDirect(() -> {
//											e.getRight().run();
//											return null;
//										}));
//									}
//								}
//							}
//						});
//					}
//				});
		}

//		private CompletableFuture<Void> subscribeConnected(String channel)
//		{
//			return connection.async().subscribe(channel).toCompletableFuture();
//		}
	}
}
