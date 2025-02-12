package cz.znj.kvr.sw.exp.java.corejava.io.bytes;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertEquals;


/**
 * Testing unpredictability of InputStream.available() .
 *
 * The tests may actually fail on some OS, as behavior is not guaranteed.
 */
public class InputStreamSizeTest
{
	@Test
	public void available_whenBigUnder2G_returnReal() throws IOException {
		File file = createFile(2_100_000_000);
		try (InputStream input = new FileInputStream(file)) {
			assertEquals(input.available(), 2_100_000_000);
		}
	}

	@Test
	public void available_whenBigOver2G_returnReal() throws IOException {
		File file = createFile(3_100_000_000L);
		try (InputStream input = new FileInputStream(file)) {
			assertEquals(input.available(), Integer.MAX_VALUE);
		}
	}

	@Test
	public void available_ByteArray_returnReal() throws IOException {
		try (InputStream input = new ByteArrayInputStream(StringUtils.repeat("Hello World, Zbynek\n", 1_000_000).getBytes(StandardCharsets.UTF_8))) {
			assertEquals(input.available(), 20_000_000);
		}
	}

	private File createFile(long size) throws IOException
	{
		byte[] bytes = new byte[1 * 1024 * 1024];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) (i * i + i);
		}
		File file = File.createTempFile("test", "bin");
		try (OutputStream out = new FileOutputStream(file)) {
			for (long off = 0; off < size; ) {
				int n = (int) Math.min(bytes.length, size - off);
				out.write(bytes, 0, n);
				off += n;
			}
		}
		file.deleteOnExit();
		return file;
	}
}
