package cz.znj.kvr.sw.exp.java.message.pubsub.pubsub.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.cmdline.command.AbstractCommand;

import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


/**
 * Simple Jedis pub-sub tester.
 *
 * The Jedis client is not synchronized, therefore must have an instance per subscriber.
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public abstract class AbstractPublishReceiveBenchmark extends AbstractCommand
{
	@Override
	public int execute() throws Exception
	{
		log.info("publishTest: START: {}", getClass().getSimpleName());

		runBenchmark();

		log.info("publishTest EXIT: {}", getClass().getSimpleName());
		return 0;
	}

	protected abstract void runBenchmark() throws Exception;

	protected void runBenchmarkLoop(AtomicInteger pending, Consumer<String> publisher)
	{
		pending.incrementAndGet();
		long count;
		long started = System.currentTimeMillis();
		for (count = 0; ; ++count) {
			pending.incrementAndGet();
			publisher.accept("Message-" + count);
			if (count%65536 == 0) {
				if (System.currentTimeMillis()-started >= 10_000) {
					break;
				}
				else if (pending.get() > 65536) {
					synchronized (pending) {
						while (pending.get() > 65536) {
							try {
								pending.wait();
							}
							catch (InterruptedException ex) {
								throw new RuntimeException(ex);
							}
						}
					}
				}
			}
		}
		if (pending.decrementAndGet() != 0) {
			synchronized (pending) {
				while (pending.get() != 0) {
					try {
						pending.wait();
					}
					catch (InterruptedException ex) {
						throw new RuntimeException(ex);
					}
				}
			}
		}
		long end = System.currentTimeMillis();
		log.info("Completed: messages={} time={} ms speed={} msg/s", count, end-started, count*1000L/(end-started));

		try (BenchmarkFormatter formatter = new BenchmarkFormatter(System.out)) {
			formatter.printBenchmark(BenchmarkFormatter.Benchmark.builder()
				.name("Redis.publishReceive")
				.mode("avgt")
				.units("ops/s")
				.score(String.valueOf(count*1000L/(end-started)))
				.build());
		}

	}
}
