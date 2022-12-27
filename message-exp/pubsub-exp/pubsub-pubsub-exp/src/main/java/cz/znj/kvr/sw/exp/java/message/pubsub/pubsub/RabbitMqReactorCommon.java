package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub;

import com.google.common.collect.ImmutableList;
import com.rabbitmq.client.Address;
import io.lettuce.core.RedisConnectionStateListener;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.SharedScheduledExecutorInstance;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


@Log4j2
@Singleton
public class RabbitMqReactorCommon implements AutoCloseable
{
	@ServerUrlInject
	private final String serverUrl;

	private Sender senderClient;

	private Receiver receiverClient;

	private final boolean autoReconnect;

	@Inject
	public RabbitMqReactorCommon(@ServerUrlInject String serverUrl)
	{
		this(serverUrl, false);
	}

	public RabbitMqReactorCommon(@ServerUrlInject String serverUrl, boolean autoReconnect)
	{
		this.serverUrl = serverUrl;
		this.autoReconnect = autoReconnect;
	}

	public synchronized Sender getPoolPublisher()
	{
		if (senderClient == null) {
			senderClient = RabbitFlux.createSender(new SenderOptions()
				.connectionSupplier(cf -> cf.newConnection(ImmutableList.of(Address.parseAddress((serverUrl)))))
			);
		}
		return senderClient;
	}

	public CompletableFuture<Sender> getPublisher()
	{
		MutableObject<Future<?>> scheduled = new MutableObject<>();
		CompletableFuture<Sender> future = new CompletableFuture<>() {
			@Override
			public boolean cancel(boolean interrupt)
			{
				scheduled.getValue().cancel(true);
				return super.cancel(interrupt);
			}
		};
		synchronized (scheduled) {
			scheduled.setValue(SharedScheduledExecutorInstance.getScheduledExecutorService().schedule(
				() -> {
//					getSender().connectPubSubAsync(new StringCodec(), RedisURI.create(serverUrl))
//						.thenAccept((connection) -> {
//							future.complete(connection);
//							synchronized (scheduled) {
//								scheduled.getValue().cancel(false);
//							}
//						});
					future.complete(getPoolPublisher());
							synchronized (scheduled) {
								scheduled.getValue().cancel(false);
							}
				},
				0,
				1,
				TimeUnit.SECONDS
			));
		}
		return future;
	}

	public synchronized Receiver getPoolReceiver()
	{
		if (receiverClient == null) {
			receiverClient = RabbitFlux.createReceiver(new ReceiverOptions()
				.connectionSupplier(cf -> cf.newConnection(ImmutableList.of(Address.parseAddress((serverUrl)))))
			);
		}
		return receiverClient;
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
		if (senderClient != null)
			senderClient.close();
	}

	public class Publisher implements AutoCloseable
	{
		private final RedisConnectionStateListener lifecycleListener;

		private Sender connection;

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
				return connection.send(Mono.just(new OutboundMessage("", channel, message.getBytes(StandardCharsets.UTF_8))))
					.then().toFuture();
			}
			else {
				return CompletableFuture.failedFuture(new IOException("Not connected"));
			}
		}

		private synchronized void reconnect()
		{
			this.connection = getPoolPublisher();
			this.reconnectFuture = CompletableFuture.completedFuture(null);
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
				connection.close();
			}
			reconnectFuture.cancel(true);
		}
	}

	public class Subscription implements AutoCloseable
	{
		private final RedisConnectionStateListener lifecycleListener;

		private Receiver receiver;

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
			return new CompletableFuture<>() {
				{
					getPoolPublisher().declareQueue(QueueSpecification.queue(channel))
						.doOnError(ex -> completeExceptionally(ex))
						.subscribe(m -> {
							receiver.consumeAutoAck(channel, new ConsumeOptions()
									.channelCallback(c -> complete(null)))
								.doOnError(ex -> completeExceptionally(ex))
								.subscribe((message) -> onMessage.accept(new String(message.getBody(), StandardCharsets.UTF_8)));
					});
				}
			};
		}

		@Override
		public void close()
		{
			try {
				if (receiver != null) {
					receiver.close();
				}
			}
			finally {
				reconnectFuture.cancel(true);
			}
		}

		private synchronized void reconnect()
		{
			receiver = getPoolReceiver();
			reconnectFuture = CompletableFuture.completedFuture(null);
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
