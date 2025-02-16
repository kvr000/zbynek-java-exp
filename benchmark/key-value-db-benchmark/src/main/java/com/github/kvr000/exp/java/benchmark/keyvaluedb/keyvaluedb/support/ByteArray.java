package com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb.support;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Byte array wrapper, with simple hash and equals.
 */
public class ByteArray implements Serializable
{
	private final byte[] array;

	public ByteArray(byte[] array) {
		this.array = array;
	}

	@Override
	public int hashCode() {
		int h = 3;
		for (int i = 0; i < array.length; ++i) {
			h = h*17+array[i];
		}
		return h;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ByteArray) {
			return Arrays.equals(((ByteArray) o).array, this.array);
		}
		return false;
	}
}
