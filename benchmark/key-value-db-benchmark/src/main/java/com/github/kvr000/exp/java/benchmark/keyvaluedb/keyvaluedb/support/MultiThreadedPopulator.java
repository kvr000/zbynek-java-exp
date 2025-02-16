package com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb.support;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Database populator.
 */
@Log4j2
public class MultiThreadedPopulator implements Populator
{
	public void populate(long numItems, Function<Long, CloseableConsumer<Long>> writerSupplier) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		int processors = Runtime.getRuntime().availableProcessors();

		ExecutorService executor = Executors.newFixedThreadPool(processors);
		List<Future<Void>> futures = new ArrayList<>();
		for (int p = 0; p < processors; ++p) {
			int cp = p;
			futures.add(executor.submit(() -> {
				try (CloseableConsumer<Long> writer = writerSupplier.apply((long)cp)) {
					for (long i = cp; i < numItems; i += processors) {
						writer.accept(i);
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
		log.info("Populating done in "+stopWatch.getTime()+" ms");
	}
}
