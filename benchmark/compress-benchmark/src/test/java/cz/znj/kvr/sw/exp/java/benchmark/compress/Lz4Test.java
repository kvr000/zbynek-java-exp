package cz.znj.kvr.sw.exp.java.benchmark.compress;

import cz.znj.kvr.sw.exp.java.benchmark.compress.support.CompressBenchmarkSupport;
import lombok.extern.log4j.Log4j2;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;


@Log4j2
public class Lz4Test
{
	public static final int NUM_THREADS = 2;

	public static final int TEST_SIZE = 10_000_000;

	private static final byte[] sequence = CompressBenchmarkSupport.generateInput(TEST_SIZE);

	@Test
	public void benchmarkLz4JavaSingle() throws IOException
	{
		try (
				ByteArrayOutputStream captureStream = new ByteArrayOutputStream();
				OutputStream compressStream = new LZ4BlockOutputStream(captureStream)
		) {
			IOUtils.write(sequence, compressStream);
		}
	}

	@Test
	public void benchmarkLz4JavaMulti() throws IOException
	{
		CompressBenchmarkSupport.runParallel(NUM_THREADS, (Integer id) -> {
			try (
					ByteArrayOutputStream captureStream = new ByteArrayOutputStream();
					OutputStream compressStream = new LZ4BlockOutputStream(captureStream)
			) {
				IOUtils.write(sequence, compressStream);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	public void benchmarkLz4CommonsBlockSingle() throws IOException
	{
		try (
				ByteArrayOutputStream captureStream = new ByteArrayOutputStream();
				OutputStream compressStream = new BlockLZ4CompressorOutputStream(captureStream)
		) {
			IOUtils.write(sequence, compressStream);
		}
	}

	@Test
	public void benchmarkLz4CommonsBlockMulti() throws IOException
	{
		CompressBenchmarkSupport.runParallel(NUM_THREADS, (Integer id) -> {
			try (
					ByteArrayOutputStream captureStream = new ByteArrayOutputStream();
					OutputStream compressStream = new BlockLZ4CompressorOutputStream(captureStream)
			) {
				IOUtils.write(sequence, compressStream);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	public void benchmarkLz4CommonsFramedSingle() throws IOException
	{
		try (
				ByteArrayOutputStream captureStream = new ByteArrayOutputStream();
				OutputStream compressStream = new FramedLZ4CompressorOutputStream(captureStream)
		) {
			IOUtils.write(sequence, compressStream);
		}
	}

	@Test
	public void benchmarkLz4CommonsFramedMulti() throws IOException
	{
		CompressBenchmarkSupport.runParallel(NUM_THREADS, (Integer id) -> {
			try (ByteArrayOutputStream captureStream = new ByteArrayOutputStream()) {
				try (OutputStream compressStream = new FramedLZ4CompressorOutputStream(captureStream)) {
					IOUtils.write(sequence, compressStream);
				}
				Assert.assertNotEquals(0, captureStream.size());
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

}
