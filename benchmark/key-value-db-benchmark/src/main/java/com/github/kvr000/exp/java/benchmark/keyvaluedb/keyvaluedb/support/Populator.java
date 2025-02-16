package com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb.support;

import java.util.function.Function;

/**
 * Database populator.
 */
public interface Populator
{
	void populate(long numItems, Function<Long, CloseableConsumer<Long>> writerSupplier);
}
