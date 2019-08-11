package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.util;

import java.util.Map;
import java.util.function.Function;


/**
 *
 */
public class MapLookupFunction<K, V> implements Function<K, V>
{
	public MapLookupFunction(Map<K, V> values)
	{
		this.values = values;
	}

	@Override
	public V apply(K k)
	{
		V value = values.get(k);
		if (value == null) {
			throw new IllegalArgumentException("Key does not exist: "+k);
		}
		return value;
	}

	private final Map<K, V> values;
}
