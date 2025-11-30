package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub.benchmark;

import cz.znj.kvr.sw.exp.java.message.pubsub.pubsub.LettuceCommon;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisConnectionStateAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import jakarta.inject.Inject;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;


@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class LettuceSubscribeCommand extends AbstractSubscribeCommand
{
	private final LettuceCommon common;

	@Override
	public int execute() throws Exception
	{
		CountDownLatch exitLatch = new CountDownLatch(options.count);
		List<LettuceCommon.Subscription> listeners = new ArrayList<>();
		CompletableFuture<Void> all = CompletableFuture.completedFuture(null);
		for (int i = 0; i < options.count; ++i) {
			final int i0 = i;
			LettuceCommon.Subscription subscription = common.createSubscription(new RedisConnectionStateAdapter() {
				@Override
				public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress)
				{
					log.info("Reconnected subscriber");
				}
			});
			all = all.runAfterBoth(subscription.subscribe("Channel-bench", this::onReceived, () -> {
				log.info("Connected: id={}", i0);
			}), () -> {});
			listeners.add(subscription);
		}
		all.get();
		log.info("Running subscribers: count={}", options.count);
		exitLatch.await();
		return 0;
	}
}
