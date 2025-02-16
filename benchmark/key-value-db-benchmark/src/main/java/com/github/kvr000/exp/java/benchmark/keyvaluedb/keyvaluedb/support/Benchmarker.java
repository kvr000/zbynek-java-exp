package com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb.support;

import java.util.function.Function;

/**
 * Benchmark runner.
 */
public interface Benchmarker
{
	void benchmark(long steps, long numItems, Function<Long, CloseableConsumer<Long>> performerSupplier);
}
