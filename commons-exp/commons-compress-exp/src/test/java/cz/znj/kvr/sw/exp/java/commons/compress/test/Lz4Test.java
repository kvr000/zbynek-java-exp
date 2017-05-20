package cz.znj.kvr.sw.exp.java.commons.compress.test;

import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class Lz4Test
{
	public static final long FILE_SIZE = 11_000_000;

	@Test
	public void testLz4FrameOutput() throws IOException {

		byte[] plain;
		byte[] compressed;
		try (
				ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
				ByteArrayOutputStream plainStream = new ByteArrayOutputStream()
		) {
			try (OutputStream randomOutput = new FramedLZ4CompressorOutputStream(compressedStream)) {
				for (int i = 0; i < FILE_SIZE; i += 1_000_000) {
					byte[] block = RandomUtils.nextBytes(1_000_000);
					plainStream.write(block);
					randomOutput.write(block);
				}
			}
			plain = plainStream.toByteArray();
			compressed = compressedStream.toByteArray();

//			Path lz4Path = Files.createTempFile("lz4file", ".lz4");
//			lz4Path.toFile().deleteOnExit();
//			FileUtils.writeByteArrayToFile(lz4Path.toFile(), compressed);
		}

		try (InputStream input = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(compressed))) {
			input.skip(plain.length);
			Assert.assertEquals(-1, input.read());
		}
	}

	@Test
	public void testLz4BlockOutput() throws IOException {

		byte[] plain;
		byte[] compressed;
		try (
				ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
				ByteArrayOutputStream plainStream = new ByteArrayOutputStream()
		) {
			try (OutputStream randomOutput = new BlockLZ4CompressorOutputStream(compressedStream)) {
				for (int i = 0; i < FILE_SIZE; i += 1_000_000) {
					byte[] block = RandomUtils.nextBytes(1_000_000);
					plainStream.write(block);
					randomOutput.write(block);
				}
			}
			plain = plainStream.toByteArray();
			compressed = compressedStream.toByteArray();

			Path lz4Path = Files.createTempFile("lz4file", ".lz4");
			lz4Path.toFile().deleteOnExit();
			FileUtils.writeByteArrayToFile(lz4Path.toFile(), compressed);
		}

		try (InputStream input = new BlockLZ4CompressorInputStream(new ByteArrayInputStream(compressed))) {
			input.skip(plain.length);
			Assert.assertEquals(-1, input.read());
		}
	}
}
