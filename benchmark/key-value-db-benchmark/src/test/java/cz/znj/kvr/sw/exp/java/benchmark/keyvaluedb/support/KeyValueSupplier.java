package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support;


import java.io.Closeable;

/**
 * Supplier of key and value.
 */
public interface KeyValueSupplier extends Closeable
{
	byte[] generateKey(long id);

	byte[] generateValue(long id);
}
