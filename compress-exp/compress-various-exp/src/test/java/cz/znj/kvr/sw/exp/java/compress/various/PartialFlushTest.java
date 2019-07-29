package cz.znj.kvr.sw.exp.java.compress.various;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class PartialFlushTest
{
	public static final String STR1 = "Hello\n";

	@Test
	public void gzipPartialUnflushed() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		GZIPOutputStream compressOutput = new GZIPOutputStream(output, true);
		Writer writer = new OutputStreamWriter(compressOutput, StandardCharsets.UTF_8);
		writer.write(STR1);
		NonblockingInputStream input = new NonblockingInputStream();
		moveOutput(output, input);
		ByteBuffer cache = ByteBuffer.allocate(1).flip();
		Assert.expectThrows(NonblockingInputStream.BlockedIoException.class, () -> {
			GZIPInputStream compressInput = new GZIPInputStream(input);
			readLine(cache, compressInput);
		});
	}

	@Test
	public void gzipPartialFlushed() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		GZIPOutputStream compressOutput = new GZIPOutputStream(output, true);
		Writer writer = new OutputStreamWriter(compressOutput, StandardCharsets.UTF_8);
		writer.write(STR1);
		writer.flush();
		NonblockingInputStream input = new NonblockingInputStream();
		moveOutput(output, input);
		GZIPInputStream compressInput = new GZIPInputStream(input);
		ByteBuffer cache = ByteBuffer.allocate(1).flip();
		String out1 = readLine(cache, compressInput);
		Assert.assertEquals(out1, STR1);
	}

	@Test
	public void zstdPartialUnflushed() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ZstdOutputStream compressOutput = new ZstdOutputStream(output);
		Writer writer = new OutputStreamWriter(compressOutput, StandardCharsets.UTF_8);
		writer.write(STR1);
		NonblockingInputStream input = new NonblockingInputStream();
		moveOutput(output, input);
		Assert.expectThrows(NonblockingInputStream.BlockedIoException.class, () -> {
			ZstdInputStream compressInput = new ZstdInputStream(input);
			compressInput.setContinuous(true);
			ByteBuffer cache = ByteBuffer.allocate(1).flip();
			readLine(cache, compressInput);
		});
	}

	@Test
	public void zstdPartialFlushedOneByte() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ZstdOutputStream compressOutput = new ZstdOutputStream(output);
		Writer writer = new OutputStreamWriter(compressOutput, StandardCharsets.UTF_8);
		writer.write(STR1);
		writer.flush();
		NonblockingInputStream input = new NonblockingInputStream();
		ZstdInputStream compressInput = new ZstdInputStream(input);
		compressInput.setContinuous(true);
		moveOutput(output, input);
		ByteBuffer cache = ByteBuffer.allocate(1).flip();
		String out1 = readLine(cache, compressInput);
		Assert.assertEquals(out1, STR1);
	}

	@Test
	public void zstdPartialFlushedBuffered() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ZstdOutputStream compressOutput = new ZstdOutputStream(output);
		Writer writer = new OutputStreamWriter(compressOutput, StandardCharsets.UTF_8);
		writer.write(STR1);
		writer.flush();
		NonblockingInputStream input = new NonblockingInputStream();
		ZstdInputStream compressInput = new ZstdInputStream(input);
		compressInput.setContinuous(true);
		moveOutput(output, input);
		ByteBuffer cache = ByteBuffer.allocate(1024).flip();
		String out1 = readLine(cache, compressInput);
		Assert.assertEquals(out1, STR1);
	}

	@Test
	public void zstdPartialContinuousOneByte() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ZstdOutputStream compressOutput = new ZstdOutputStream(output);
		Writer writer = new OutputStreamWriter(compressOutput, StandardCharsets.UTF_8);
		writer.write(STR1);
		NonblockingInputStream input = new NonblockingInputStream();
		ZstdInputStream compressInput = new ZstdInputStream(input);
		compressInput.setContinuous(true);
		moveOutput(output, input);
		ByteBuffer cache = ByteBuffer.allocate(1).flip();
		Assert.expectThrows(NonblockingInputStream.BlockedIoException.class, () -> readLine(cache, compressInput));
		writer.close();
		moveOutput(output, input);
		input.finishInput();
		String out1 = readLine(cache, compressInput);
		Assert.assertEquals(out1, STR1);
		String out2 = readLine(cache, compressInput);
		Assert.assertEquals(out2, null);
	}

	@Test
	public void zstdPartialContinuousBuffered() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ZstdOutputStream compressOutput = new ZstdOutputStream(output);
		Writer writer = new OutputStreamWriter(compressOutput, StandardCharsets.UTF_8);
		writer.write(STR1);
		NonblockingInputStream input = new NonblockingInputStream();
		ZstdInputStream compressInput = new ZstdInputStream(input);
		compressInput.setContinuous(true);
		moveOutput(output, input);
		ByteBuffer cache = ByteBuffer.allocate(1024).flip();
		Assert.expectThrows(NonblockingInputStream.BlockedIoException.class, () -> readLine(cache, compressInput));
		writer.close();
		moveOutput(output, input);
		input.finishInput();
		String out1 = readLine(cache, compressInput);
		Assert.assertEquals(out1, STR1);
		String out2 = readLine(cache, compressInput);
		Assert.assertEquals(out2, null);
	}

	// Bufferring possible here, but no overbufferring (like BufferedReader does, in a loop)
	private String readLine(ByteBuffer cache, InputStream input) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		int b;
		while ((b = readCharacter(cache, input)) >= 0) {
			sb.append((char) b); // fix, but we have ASCII in tests now
			if (b == '\n') {
				return sb.toString();
			}
		}
		return sb.length() > 0 ? sb.toString() : null;
	}

	private int readCharacter(ByteBuffer cache, InputStream input) throws IOException
	{
		// simplified for purpose of unit tests, does not handle non-ASCII correctly
		if (cache.hasRemaining()) {
			return cache.get();
		}
		cache.clear();
		cache.flip();
		int read = input.read(cache.array(), cache.position(), cache.capacity());
		if (read < 0) {
			return -1;
		}
		cache.limit(read);
		return cache.get();
	}

	private void moveOutput(ByteArrayOutputStream output, NonblockingInputStream input) throws IOException
	{
		input.addInput(output.toByteArray());
		output.reset();
	}

	private static class NonblockingInputStream extends InputStream
	{
		private boolean finished;

		private ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

		public NonblockingInputStream()
		{
		}

		public synchronized void addInput(byte[] bytes) throws IOException {
			this.input = new ByteArrayInputStream(ArrayUtils.addAll(IOUtils.toByteArray(input), bytes));
		}

		public void finishInput()
		{
			this.finished = true;
		}

		@Override
		public synchronized int read() throws IOException
		{
			int b = input.read();
			if (b < 0) {
				if (!finished) {
					throw new BlockedIoException("No bytes available yet");
				}
			}
			return b;
		}

		@Override
		public synchronized int read(byte[] buf, int offset, int length) throws IOException
		{
			int b = input.read(buf, offset, length);
			if (b <= 0) {
				if (!finished) {
					throw new BlockedIoException("No bytes available yet");
				}
			}
			return b;
		}

		public static class BlockedIoException extends IOException
		{
			public BlockedIoException(String message)
			{
				super(message);
			}

			public BlockedIoException(String message, Throwable cause)
			{
				super(message, cause);
			}

			public BlockedIoException(Throwable cause)
			{
				super(cause);
			}
		}
	}
}
