package cz.znj.kvr.sw.exp.java.message.pubsub.jedis.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.cmdline.command.CommandContext;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;


@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class SubscribeCommand extends AbstractCommand
{
	private final Common common;

	private final ExecutorService blockingExecutor;

	private final JedisBenchmarkRunner.Options mainOptions;

	private final Options options = new Options();

	@Override
	protected boolean parseOption(CommandContext context, String arg, ListIterator<String> args) throws Exception
	{
		switch (arg) {
		case "-c":
			this.options.count = Integer.parseInt(needArgsParam(this.options.count == 0 ? null : 1, args));
			return true;

		default:
			return super.parseOption(context, arg, args);
		}
	}

	@Override
	protected int validateOptions(CommandContext context, ListIterator<String> args) throws Exception
	{
		if (options.count <= 0) {
			return usage(context, "-c must be positive");
		}
		return EXIT_CONTINUE;
	}

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

	public static class Options
	{
		int count;
	}
}
