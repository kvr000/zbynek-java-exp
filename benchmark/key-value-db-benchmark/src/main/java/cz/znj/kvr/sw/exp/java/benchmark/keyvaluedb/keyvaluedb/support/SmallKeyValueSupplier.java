package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.keyvaluedb.support;


/**
 * Small data supplier of key and value.
 */
public class SmallKeyValueSupplier extends AbstractKeyValueSupplier
{
	public byte[] generateKey(long id) {
		return ("Zbynek"+id).getBytes();
	}

	public byte[] generateValue(long id) {
		return ("Vyskovsky"+id).getBytes();
	}
}
