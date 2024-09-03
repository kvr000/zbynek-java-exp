package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.keyvaluedb.support;

import org.apache.commons.lang3.RandomUtils;

import java.io.IOException;
import java.util.function.Function;

/**
 * Sequential, single threaded, benchmarker.
 */
public class SequentialSingleThreadedBenchmarker implements Benchmarker
{
	@Override
	public void benchmark(long steps, long numItems, Function<Long, CloseableConsumer<Long>> writerSupplier) {
		long start = RandomUtils.nextLong(0, numItems-steps);
		try (CloseableConsumer<Long> writer = writerSupplier.apply(0L)) {
			for (long i = 0; i < steps; ++i) {
				writer.accept(start+i);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
