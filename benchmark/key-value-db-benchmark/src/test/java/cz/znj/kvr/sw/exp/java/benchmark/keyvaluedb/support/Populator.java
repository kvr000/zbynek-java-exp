package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support;

import java.io.Closeable;
import java.util.function.Function;

/**
 * Database populator.
 */
public interface Populator
{
	void populate(long numItems, Function<Long, CloseableConsumer<Long>> writerSupplier);
}
