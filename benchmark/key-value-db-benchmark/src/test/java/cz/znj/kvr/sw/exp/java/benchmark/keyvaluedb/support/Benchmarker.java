package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support;

import java.util.function.Function;

/**
 * Benchmark runner.
 */
public interface Benchmarker
{
	void benchmark(long steps, long numItems, Function<Long, CloseableConsumer<Long>> performerSupplier);
}
