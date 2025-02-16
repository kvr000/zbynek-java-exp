package com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb.support;

import java.io.Closeable;

/**
 * Object stream writer.
 */
public interface CloseableConsumer<T> extends Closeable
{
	void accept(T object);
}
