package cz.znj.kvr.sw.exp.java.message.pubsub.redis.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;


@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class JedisSubscribeCommand extends AbstractSubscribeCommand
{
	private final JedisCommon common;

	private final ExecutorService blockingExecutor;

	@Override
	public int execute() throws Exception
	{
		CountDownLatch exitLatch = new CountDownLatch(options.count);
		List<CompletableFuture<JedisPubSub>> listeners = new ArrayList<>();
		for (int i = 0; i < options.count; ++i) {
			CompletableFuture<JedisPubSub> listener = new CompletableFuture<>();
			blockingExecutor.execute(() -> {
				try (Jedis jedis = common.createJedis()) {
					jedis.subscribe(new JedisPubSub()
							{
								@Override
								public void onSubscribe(String channel, int count)
								{
									log.info("subscribed to: " +
											"channel={} count={}", channel
										, count);
									listener.complete(this);
								}

								@Override
								public void onMessage(String channel,
										      String message)
								{
								}
							},
						"Channel-1");
					log.info("Exiting Channel-1  listener");
				}
				catch (Throwable ex) {
					listener.completeExceptionally(ex);
				}
				finally {
					exitLatch.countDown();
				}
			});
			listeners.add(listener);
		}
		listeners.forEach(CompletableFuture::join);
		exitLatch.await();
		return 0;
	}
}
