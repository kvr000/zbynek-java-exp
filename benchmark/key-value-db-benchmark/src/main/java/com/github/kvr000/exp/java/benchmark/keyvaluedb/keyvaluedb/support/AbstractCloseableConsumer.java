package com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb.support;

import java.io.IOException;

/**
 * Object stream writer, partial implementation.
 */
public abstract class AbstractCloseableConsumer<T> implements CloseableConsumer<T>
{
	@Override
	public void close() throws IOException {
	}
}
