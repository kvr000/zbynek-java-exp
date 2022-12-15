package cz.znj.kvr.sw.exp.java.message.pubsub.jedis.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.concurrent.executor.BatchWorkExecutor;
import net.dryuf.concurrent.executor.ClosingExecutor;
import net.dryuf.concurrent.executor.WorkExecutor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Simple Jedis pub-sub tester.
 *
 * The Jedis client is not synchronized, therefore must have an instance per subscriber.
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class JedisWorkPoolPublishReceiveBenchmark extends AbstractCommand
{
	private final Common common;

	@Override
	public int execute() throws Exception
	{
		try (WorkExecutor<Consumer<Jedis>, Void> jedisExecutor = setupJedis()) {
			log.info("publishTest: START");

			AtomicInteger pending = new AtomicInteger(1);
			CountDownLatch exitLatch = new CountDownLatch(1);
			JedisPubSub listener = common.createListener("Channel-1", exitLatch, pending);

			long count;
			long started = System.currentTimeMillis();
			for (count = 0; ; ++count) {
				pending.incrementAndGet();
				jedisExecutor.submit(j -> j.publish("Channel-1", "Message"));
				if (count%65536 == 0) {
					if (System.currentTimeMillis()-started >= 10_000) {
						break;
					}
					else if (pending.get() > 65536) {
						synchronized (pending) {
							while (pending.get() > 65536) {
								pending.wait();
							}
						}
					}
				}
			}
			if (pending.decrementAndGet() != 0) {
				synchronized (pending) {
					while (pending.get() != 0) {
						pending.wait();
					}
				}
			}
			long end = System.currentTimeMillis();
			log.info("Completed: messages={} time={} ms speed={} msg/s", count, end-started, count*1000L/(end-started));

			try (BenchmarkFormatter formatter = new BenchmarkFormatter(System.out)) {
				formatter.printBenchmark(BenchmarkFormatter.Benchmark.builder()
					.name("Jedis.publishReceive")
					.mode("avgt")
					.units("ops/s")
					.score(String.valueOf(count*1000L/(end-started)))
					.build());
			}

			listener.unsubscribe();

			exitLatch.await();

			log.info("publishTest EXIT");
		}
		return 0;
	}

	public WorkExecutor<Consumer<Jedis>, Void> setupJedis()
	{
		JedisPool jedisPool = common.createJedisPool();
		return new BatchWorkExecutor<>(
			new ClosingExecutor(Executors.newFixedThreadPool(10*Runtime.getRuntime().availableProcessors())),
			100,
			(List<Consumer<Jedis>> items) -> {
				try (Jedis jedis = jedisPool.getResource()) {
					//log.info("Processing messages: count={}", items.size());
					return items.stream()
						.map(item -> {
							item.accept(jedis);
							return CompletableFuture.completedFuture((Void) null);
						})
						.collect(Collectors.toList());
				}
			}
		);
	}
}
