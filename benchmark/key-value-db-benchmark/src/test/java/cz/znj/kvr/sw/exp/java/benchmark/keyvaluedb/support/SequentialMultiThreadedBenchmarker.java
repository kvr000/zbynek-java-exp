package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support;

import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Sequential, multi threaded, benchmarker.
 */
public class SequentialMultiThreadedBenchmarker implements Benchmarker
{
	@Override
	public void benchmark(long steps, long numItems, Function<Long, CloseableConsumer<Long>> performerSupplier) {
		int processors = Runtime.getRuntime().availableProcessors();

		ExecutorService executor = Executors.newFixedThreadPool(processors);
		List<Future<Void>> futures = new ArrayList<>();
		long start = RandomUtils.nextLong(0, numItems-steps-processors);
		for (int p = 0; p < processors; ++p) {
			int cp = p;
			futures.add(executor.submit(() -> {
				try (CloseableConsumer<Long> performer = performerSupplier.apply((long)cp)) {
					for (long i = cp; i < steps; i += processors) {
						performer.accept(start+i);
					}
				}
				return (Void)null;
			}));
		}
		futures.forEach((Future<Void> future) -> {
			try {
				future.get();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		});
		executor.shutdown();
	}
}
