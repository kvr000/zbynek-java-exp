package cz.znj.kvr.sw.exp.java.nio.socket.test;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;


/**
 * Created by vyskovsz on 09/10/2015.
 */
public class ByteBufferExp
{
	@Test
	public void                     testRewinds()
	{
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.putInt(0x12345678);
		ByteBuffer extended = ByteBuffer.allocate(1024);
		Assert.assertEquals(0, extended.position());
		buffer.flip();
		buffer.get();
		extended.put(buffer);
		Assert.assertEquals(3, extended.position());
		extended.put((byte)0x69);
		Assert.assertEquals(4, extended.position());
	}
}
