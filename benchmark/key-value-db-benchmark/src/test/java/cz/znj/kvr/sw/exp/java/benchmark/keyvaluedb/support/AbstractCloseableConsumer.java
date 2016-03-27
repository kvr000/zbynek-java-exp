package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support;

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
