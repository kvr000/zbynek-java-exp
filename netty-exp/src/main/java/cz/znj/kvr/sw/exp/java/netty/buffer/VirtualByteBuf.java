package cz.znj.kvr.sw.exp.java.netty.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.SwappedByteBuf;
import io.netty.util.ByteProcessor;
import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;


/**
 * Created by vyskovsz on 13/10/2015.
 */
public class VirtualByteBuf extends ByteBuf
{
	@Override
	public int capacity()
	{
		return 0;
	}

	@Override
	public ByteBuf capacity(int newCapacity)
	{
		return this;
	}

	@Override
	public int maxCapacity()
	{
		return 0;
	}

	@Override
	public ByteBufAllocator alloc()
	{
		return null;
	}

	@Override
	public ByteOrder order()
	{
		return ByteOrder.BIG_ENDIAN;
	}

	@Override
	public ByteBuf order(ByteOrder endianness)
	{
		return new SwappedByteBuf(this);
	}

	@Override
	public ByteBuf unwrap()
	{
		return this;
	}

	@Override
	public boolean isDirect()
	{
		return false;
	}

	@Override
	public int readerIndex()
	{
		return readerIndex();
	}

	@Override
	public ByteBuf readerIndex(int readerIndex)
	{
		this.readerIndex = readerIndex;
		return this;
	}

	@Override
	public int writerIndex()
	{
		return writerIndex;
	}

	@Override
	public ByteBuf writerIndex(int writerIndex)
	{
		this.writerIndex = writerIndex;
		return this;
	}

	@Override
	public ByteBuf setIndex(int readerIndex, int writerIndex)
	{
		readerIndex(readerIndex);
		writerIndex(writerIndex);
		return this;
	}

	@Override
	public int readableBytes()
	{
		return writerIndex-readerIndex;
	}

	@Override
	public int writableBytes()
	{
		return 0;
	}

	@Override
	public int maxWritableBytes()
	{
		return 0;
	}

	@Override
	public boolean isReadable()
	{
		return readerIndex < writerIndex;
	}

	@Override
	public boolean isReadable(int size)
	{
		return readerIndex+size < writerIndex;
	}

	@Override
	public boolean isWritable()
	{
		return true;
	}

	@Override
	public boolean isWritable(int size)
	{
		return true;
	}

	@Override
	public ByteBuf clear()
	{
		array = new byte[0];
		return this;
	}

	@Override
	public ByteBuf markReaderIndex()
	{
		this.markedReaderIndex = this.readerIndex;
		return this;
	}

	@Override
	public ByteBuf resetReaderIndex()
	{
		this.readerIndex = this.markedReaderIndex;
		return this;
	}

	@Override
	public synchronized ByteBuf markWriterIndex()
	{
		this.markedWriterIndex = this.writerIndex;
		return this;
	}

	@Override
	public synchronized ByteBuf resetWriterIndex()
	{
		this.writerIndex = this.markedWriterIndex;
		return this;
	}

	@Override
	public synchronized ByteBuf discardReadBytes()
	{
		ArrayUtils.subarray(array, readerIndex, array.length);
		return this;
	}

	@Override
	public ByteBuf discardSomeReadBytes()
	{
		return discardReadBytes();
	}

	@Override
	public ByteBuf ensureWritable(int minWritableBytes)
	{
		return this;
	}

	@Override
	public int ensureWritable(int minWritableBytes, boolean force)
	{
		return 0;
	}

	@Override
	public boolean getBoolean(int index)
	{
		ensureSize(index+1);
		return array[index] != 0;
	}

	@Override
	public byte getByte(int index)
	{
		ensureSize(index+1);
		return array[index];
	}

	@Override
	public short getUnsignedByte(int index)
	{
		ensureSize(index+1);
		return (short)(array[index]&0xff);
	}

	@Override
	public short getShort(int index)
	{
		ensureSize(index+2);
		return (short)((array[index]<<8&0xff00)+(array[index+1]&0xff));
	}

	@Override
	public int getUnsignedShort(int index)
	{
		ensureSize(index+2);
		return ((array[index]<<8&0xff00)+(array[index+1]&0xff));
	}

	@Override
	public int getMedium(int index)
	{
		ensureSize(index+3);
		return ((array[index]<<16&0xff0000)+(array[index]<<8&0xff00)+(array[index+2]&0xff))<<8>>8;
	}

	@Override
	public int getUnsignedMedium(int index)
	{
		ensureSize(index+3);
		return ((array[index]<<16&0xff0000)+(array[index]<<8&0xff00)+(array[index+2]&0xff));
	}

	@Override
	public int getInt(int index)
	{
		ensureSize(index+4);
		return ((array[index]<<24&0xff000000)+(array[index+1]<<16&0xff0000)+(array[index+2]<<8&0xff00)+(array[index+3]&0xff));
	}

	@Override
	public long getUnsignedInt(int index)
	{
		ensureSize(index+4);
		return ((array[index]<<24&0xff000000)+(array[index+1]<<16&0xff0000)+(array[index+2]<<8&0xff00)+(array[index+3]&0xff))&0xffffffffL;
	}

	@Override
	public long getLong(int index)
	{
		ensureSize(index+8);
		return (
			(array[index]<<56&0xff00000000000000L)+(array[index+1]<<48&0xff000000000000L)+(array[index+2]<<40&0xff0000000000L)+(array[index+3]<<32&0xff00000000L)+
			(array[index+4]<<24&0xff000000)+(array[index+5]<<16&0xff0000)+(array[index+6]<<8&0xff00)+(array[index+7]&0xff)
		);
	}

	@Override
	public char getChar(int index)
	{
		return (char)getUnsignedShort(index);
	}

	@Override
	public float getFloat(int index)
	{
		return Float.intBitsToFloat(getInt(index));
	}

	@Override
	public double getDouble(int index)
	{
		return Double.longBitsToDouble(getLong(index));
	}

	@Override
	public ByteBuf getBytes(int index, ByteBuf dst)
	{
		getBytes(index, dst, array.length-index);
		return this;
	}

	@Override
	public ByteBuf getBytes(int index, ByteBuf dst, int length)
	{
		ensureSize(index+length);
		return dst.writeBytes(array, index, length);
	}

	@Override
	public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
	{
		ensureSize(index+length);
		dst.setBytes(dstIndex, array, index, length);
		return this;
	}

	@Override
	public ByteBuf getBytes(int index, byte[] dst)
	{
		return getBytes(index, dst, 0, writerIndex-index);
	}

	// TODO: finished here
	@Override
	public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
	{
		ensureSize(index+length);

		return this;
	}

	@Override
	public ByteBuf getBytes(int index, ByteBuffer dst)
	{
		return null;
	}

	@Override
	public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException
	{
		return null;
	}

	@Override
	public int getBytes(int index, GatheringByteChannel out, int length) throws IOException
	{
		return 0;
	}

	@Override
	public ByteBuf setBoolean(int index, boolean value)
	{
		return null;
	}

	@Override
	public ByteBuf setByte(int index, int value)
	{
		return null;
	}

	@Override
	public ByteBuf setShort(int index, int value)
	{
		return null;
	}

	@Override
	public ByteBuf setMedium(int index, int value)
	{
		return null;
	}

	@Override
	public ByteBuf setInt(int index, int value)
	{
		return null;
	}

	@Override
	public ByteBuf setLong(int index, long value)
	{
		return null;
	}

	@Override
	public ByteBuf setChar(int index, int value)
	{
		return null;
	}

	@Override
	public ByteBuf setFloat(int index, float value)
	{
		return null;
	}

	@Override
	public ByteBuf setDouble(int index, double value)
	{
		return null;
	}

	@Override
	public ByteBuf setBytes(int index, ByteBuf src)
	{
		return null;
	}

	@Override
	public ByteBuf setBytes(int index, ByteBuf src, int length)
	{
		return null;
	}

	@Override
	public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
	{
		return null;
	}

	@Override
	public ByteBuf setBytes(int index, byte[] src)
	{
		return null;
	}

	@Override
	public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
	{
		return null;
	}

	@Override
	public ByteBuf setBytes(int index, ByteBuffer src)
	{
		return null;
	}

	@Override
	public int setBytes(int index, InputStream in, int length) throws IOException
	{
		return 0;
	}

	@Override
	public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException
	{
		return 0;
	}

	@Override
	public ByteBuf setZero(int index, int length)
	{
		return null;
	}

	@Override
	public boolean readBoolean()
	{
		return false;
	}

	@Override
	public byte readByte()
	{
		return 0;
	}

	@Override
	public short readUnsignedByte()
	{
		return 0;
	}

	@Override
	public short readShort()
	{
		return 0;
	}

	@Override
	public int readUnsignedShort()
	{
		return 0;
	}

	@Override
	public int readMedium()
	{
		return 0;
	}

	@Override
	public int readUnsignedMedium()
	{
		return 0;
	}

	@Override
	public int readInt()
	{
		return 0;
	}

	@Override
	public long readUnsignedInt()
	{
		return 0;
	}

	@Override
	public long readLong()
	{
		return 0;
	}

	@Override
	public char readChar()
	{
		return 0;
	}

	@Override
	public float readFloat()
	{
		return 0;
	}

	@Override
	public double readDouble()
	{
		return 0;
	}

	@Override
	public ByteBuf readBytes(int length)
	{
		return null;
	}

	@Override
	public ByteBuf readSlice(int length)
	{
		return null;
	}

	@Override
	public ByteBuf readBytes(ByteBuf dst)
	{
		return null;
	}

	@Override
	public ByteBuf readBytes(ByteBuf dst, int length)
	{
		return null;
	}

	@Override
	public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length)
	{
		return null;
	}

	@Override
	public ByteBuf readBytes(byte[] dst)
	{
		return null;
	}

	@Override
	public ByteBuf readBytes(byte[] dst, int dstIndex, int length)
	{
		return null;
	}

	@Override
	public ByteBuf readBytes(ByteBuffer dst)
	{
		return null;
	}

	@Override
	public ByteBuf readBytes(OutputStream out, int length) throws IOException
	{
		return null;
	}

	@Override
	public int readBytes(GatheringByteChannel out, int length) throws IOException
	{
		return 0;
	}

	@Override
	public ByteBuf skipBytes(int length)
	{
		return null;
	}

	@Override
	public ByteBuf writeBoolean(boolean value)
	{
		return null;
	}

	@Override
	public ByteBuf writeByte(int value)
	{
		return null;
	}

	@Override
	public ByteBuf writeShort(int value)
	{
		return null;
	}

	@Override
	public ByteBuf writeMedium(int value)
	{
		return null;
	}

	@Override
	public ByteBuf writeInt(int value)
	{
		return null;
	}

	@Override
	public ByteBuf writeLong(long value)
	{
		return null;
	}

	@Override
	public ByteBuf writeChar(int value)
	{
		return null;
	}

	@Override
	public ByteBuf writeFloat(float value)
	{
		return null;
	}

	@Override
	public ByteBuf writeDouble(double value)
	{
		return null;
	}

	@Override
	public ByteBuf writeBytes(ByteBuf src)
	{
		return null;
	}

	@Override
	public ByteBuf writeBytes(ByteBuf src, int length)
	{
		return null;
	}

	@Override
	public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length)
	{
		return null;
	}

	@Override
	public ByteBuf writeBytes(byte[] src)
	{
		return null;
	}

	@Override
	public ByteBuf writeBytes(byte[] src, int srcIndex, int length)
	{
		return null;
	}

	@Override
	public ByteBuf writeBytes(ByteBuffer src)
	{
		return null;
	}

	@Override
	public int writeBytes(InputStream in, int length) throws IOException
	{
		return 0;
	}

	@Override
	public int writeBytes(ScatteringByteChannel in, int length) throws IOException
	{
		return 0;
	}

	@Override
	public ByteBuf writeZero(int length)
	{
		return null;
	}

	@Override
	public int indexOf(int fromIndex, int toIndex, byte value)
	{
		return 0;
	}

	@Override
	public int bytesBefore(byte value)
	{
		return 0;
	}

	@Override
	public int bytesBefore(int length, byte value)
	{
		return 0;
	}

	@Override
	public int bytesBefore(int index, int length, byte value)
	{
		return 0;
	}

	@Override
	public int forEachByte(ByteProcessor processor)
	{
		return 0;
	}

	@Override
	public int forEachByte(int index, int length, ByteProcessor processor)
	{
		return 0;
	}

	@Override
	public int forEachByteDesc(ByteProcessor processor)
	{
		return 0;
	}

	@Override
	public int forEachByteDesc(int index, int length, ByteProcessor processor)
	{
		return 0;
	}

	@Override
	public ByteBuf copy()
	{
		return null;
	}

	@Override
	public ByteBuf copy(int index, int length)
	{
		return null;
	}

	@Override
	public ByteBuf slice()
	{
		return null;
	}

	@Override
	public ByteBuf slice(int index, int length)
	{
		return null;
	}

	@Override
	public ByteBuf duplicate()
	{
		return null;
	}

	@Override
	public int nioBufferCount()
	{
		return 0;
	}

	@Override
	public ByteBuffer nioBuffer()
	{
		return null;
	}

	@Override
	public ByteBuffer nioBuffer(int index, int length)
	{
		return null;
	}

	@Override
	public ByteBuffer internalNioBuffer(int index, int length)
	{
		return null;
	}

	@Override
	public ByteBuffer[] nioBuffers()
	{
		return new ByteBuffer[0];
	}

	@Override
	public ByteBuffer[] nioBuffers(int index, int length)
	{
		return new ByteBuffer[0];
	}

	@Override
	public boolean hasArray()
	{
		return false;
	}

	@Override
	public byte[] array()
	{
		return new byte[0];
	}

	@Override
	public int arrayOffset()
	{
		return 0;
	}

	@Override
	public boolean hasMemoryAddress()
	{
		return false;
	}

	@Override
	public long memoryAddress()
	{
		return 0;
	}

	@Override
	public String toString(Charset charset)
	{
		return null;
	}

	@Override
	public String toString(int index, int length, Charset charset)
	{
		return null;
	}

	@Override
	public int hashCode()
	{
		return 0;
	}

	@Override
	public boolean equals(Object obj)
	{
		return false;
	}

	@Override
	public int compareTo(ByteBuf buffer)
	{
		return 0;
	}

	@Override
	public String toString()
	{
		return null;
	}

	@Override
	public ByteBuf retain(int increment)
	{
		return null;
	}

	@Override
	public int refCnt()
	{
		return 0;
	}

	@Override
	public ByteBuf retain()
	{
		return null;
	}

	@Override
	public ByteBuf touch()
	{
		return null;
	}

	@Override
	public ByteBuf touch(Object hint)
	{
		return null;
	}

	@Override
	public boolean release()
	{
		return false;
	}

	@Override
	public boolean release(int decrement)
	{
		return false;
	}

	protected final void            ensureSize(int size)
	{
		if (writerIndex-readerIndex < size)
			waitForBytes(size);
	}

	protected void                  waitForBytes(int size)
	{
		throw new ArrayIndexOutOfBoundsException(size-1);
	}

	protected byte[]                array = new byte[0];

	protected int                   readerIndex;

	protected int                   writerIndex;

	protected int                   markedReaderIndex;

	protected int                   markedWriterIndex;
}
