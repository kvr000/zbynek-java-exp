package cz.znj.kvr.sw.exp.java.commons.compress;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.lang3.RandomUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;


@State(value = Scope.Benchmark)
public class ReadBenchmark
{
	public static final int FILE_SIZE = 100_000_000;

	public static final int NUM_THREADS = 4;

	private Path zipPath;

	@Setup
	public void createZipFile() throws IOException {
		zipPath = Files.createTempFile("zipfile", "zip");
		zipPath.toFile().deleteOnExit();

		byte[] plain;
		byte[] gzipped;
		try (
				ByteArrayOutputStream plainStream = new ByteArrayOutputStream();
				ByteArrayOutputStream gzippedStream = new ByteArrayOutputStream();
				OutputStream randomOutput = new GZIPOutputStream(gzippedStream)) {
			for (int i = 0; i < FILE_SIZE; i += 1_000_000) {
				byte[] block = RandomUtils.nextBytes(1_000_000);
				plainStream.write(block);
				randomOutput.write(block);
			}
			plain = plainStream.toByteArray();
			gzipped = gzippedStream.toByteArray();
		}

		try (ZipArchiveOutputStream zipfile = new ZipArchiveOutputStream(zipPath.toFile())) {
			zipfile.setMethod(ZipArchiveOutputStream.STORED);

			ZipArchiveEntry entryA = new ZipArchiveEntry("s0.gz");
			entryA.setMethod(ZipArchiveEntry.STORED);
			zipfile.addRawArchiveEntry(entryA, new ByteArrayInputStream(gzipped));
			ZipArchiveEntry entryB = new ZipArchiveEntry("s1.gz");
			entryB.setMethod(ZipArchiveEntry.STORED);
			zipfile.addRawArchiveEntry(entryB, new ByteArrayInputStream(gzipped));
			ZipArchiveEntry entryC = new ZipArchiveEntry("s2.gz");
			entryC.setMethod(ZipArchiveEntry.STORED);
			zipfile.addRawArchiveEntry(entryC, new ByteArrayInputStream(gzipped));
			ZipArchiveEntry entryD = new ZipArchiveEntry("s3.gz");
			entryD.setMethod(ZipArchiveEntry.STORED);
			zipfile.addRawArchiveEntry(entryD, new ByteArrayInputStream(gzipped));
			ZipArchiveEntry entryDA = new ZipArchiveEntry("d0.txt");
			entryDA.setMethod(ZipArchiveEntry.DEFLATED);
			zipfile.putArchiveEntry(entryDA);
			zipfile.write(plain);
			zipfile.closeArchiveEntry();
			ZipArchiveEntry entryDB = new ZipArchiveEntry("d1.txt");
			entryDB.setMethod(ZipArchiveEntry.DEFLATED);
			zipfile.putArchiveEntry(entryDB);
			zipfile.write(plain);
			zipfile.closeArchiveEntry();
			ZipArchiveEntry entryDC = new ZipArchiveEntry("d2.txt");
			entryDC.setMethod(ZipArchiveEntry.DEFLATED);
			zipfile.putArchiveEntry(entryDC);
			zipfile.write(plain);
			zipfile.closeArchiveEntry();
			ZipArchiveEntry entryDD = new ZipArchiveEntry("d3.txt");
			entryDD.setMethod(ZipArchiveEntry.DEFLATED);
			zipfile.putArchiveEntry(entryDD);
			zipfile.write(plain);
			zipfile.closeArchiveEntry();
		}
	}

	public void			readStoredStream(ZipFile zipFile, int id) {
		ZipArchiveEntry entry = zipFile.getEntry(String.format("s%d.gz", id));
		try (InputStream input = zipFile.getInputStream(entry)) {
			input.skip(FILE_SIZE);
			assert input.read() == -1 : "Unexpected bytes from input";
		}
		catch (ZipException e) {
			throw new RuntimeException(e);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void			readDeflatedStream(ZipFile zipFile, int id) {
		ZipArchiveEntry entry = zipFile.getEntry(String.format("d%d.txt", id));
		try (InputStream input = zipFile.getInputStream(entry)) {
			input.skip(FILE_SIZE);
			assert input.read() == -1 : "Unexpected bytes from input";
		}
		catch (ZipException e) {
			throw new RuntimeException(e);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, batchSize = 1)
	@Fork(warmups = 1, value = 0)
	public void                     benchmarkStoredSingleThreadRead()
	{
		try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
			readStoredStream(zipFile, 0);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, batchSize = 1)
	@Fork(warmups = 1, value = 0)
	public void                     benchmarkStoredMultiThreadRead() throws Exception
	{
		try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
			ArrayList<Thread> threads = new ArrayList<Thread>();
			for (int i = 0; i < NUM_THREADS; ++i) {
				int ii = i;
				Thread thread = new Thread(() -> readStoredStream(zipFile, ii));
				threads.add(thread);
				thread.start();
			}
			for (Thread thread : threads) {
				thread.join();
			}
		}
	}

	@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, batchSize = 1)
	@Fork(warmups = 1, value = 0)
	public void                     benchmarkDeflatedSingleThreadRead()
	{
		try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
			readDeflatedStream(zipFile, 0);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, batchSize = 1)
	@Fork(warmups = 1, value = 0)
	public void                     benchmarkDeflatedMultiThreadRead() throws Exception
	{
		try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
			ArrayList<Thread> threads = new ArrayList<Thread>();
			for (int i = 0; i < NUM_THREADS; ++i) {
				int ii = i;
				Thread thread = new Thread(() -> readDeflatedStream(zipFile, ii));
				threads.add(thread);
				thread.start();
			}
			for (Thread thread : threads) {
				thread.join();
			}
		}
	}
}
