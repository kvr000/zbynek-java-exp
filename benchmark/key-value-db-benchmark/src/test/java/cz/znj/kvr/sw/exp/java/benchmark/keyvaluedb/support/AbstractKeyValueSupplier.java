package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support;


import java.io.IOException;

/**
 * Partial implementation of supplier of key and value.
 */
public abstract class AbstractKeyValueSupplier implements KeyValueSupplier
{
	public void close() throws IOException
	{
	}
}
