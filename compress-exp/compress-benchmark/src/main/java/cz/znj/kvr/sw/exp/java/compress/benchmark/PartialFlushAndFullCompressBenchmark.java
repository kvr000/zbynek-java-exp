package cz.znj.kvr.sw.exp.java.compress.benchmark;

import com.github.luben.zstd.ZstdOutputStream;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;


@Log4j2
@Fork(1)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class PartialFlushAndFullCompressBenchmark
{
	private static byte[][] INPUTS;
	private static byte[] FULL_INPUT;

	private boolean printedSize;

	static {
		INPUTS = new byte[4][];
		for (int i = 0; i < INPUTS.length; ++i) {
			try (InputStream input = PartialFlushAndFullCompressBenchmark.class.getResourceAsStream("Input"+i+".html")) {
				INPUTS[i] = IOUtils.toByteArray(input);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		FULL_INPUT = Stream.of(INPUTS).reduce(new byte[0], ArrayUtils::addAll);

		log.error("original, size={}", FULL_INPUT.length);
	}

	@Setup
	public void setup()
	{
		this.printedSize = false;
	}

	@Benchmark
	public void gzipFull(Blackhole blackhole) throws IOException
	{
		commonFull(blackhole, "gzipFull", (stream) -> {
			try {
				return new GZIPOutputStream(stream, true);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Benchmark
	public void gzipPart(Blackhole blackhole) throws IOException
	{
		commonPart(blackhole, "gzipPart", (stream) -> {
			try {
				return new GZIPOutputStream(stream, true);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Benchmark
	public void gzipSplit(Blackhole blackhole) throws IOException
	{
		commonSplit(blackhole, "gzipSplit", (stream) -> {
			try {
				return new GZIPOutputStream(stream, true);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Benchmark
	public void zstdFull(Blackhole blackhole) throws IOException
	{
		commonFull(blackhole, "zstdFull", (stream) -> {
			try {
				return new ZstdOutputStream(stream)
						;
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Benchmark
	public void zstdPart(Blackhole blackhole) throws IOException
	{
		commonPart(blackhole, "zstdPart", (stream) -> {
			try {
				return new ZstdOutputStream(stream)
						;
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Benchmark
	public void zstdSplit(Blackhole blackhole) throws IOException
	{
		commonSplit(blackhole, "zstdSplit", (stream) -> {
			try {
				return new ZstdOutputStream(stream)
						;
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Benchmark
	public void lz4bFull(Blackhole blackhole) throws IOException
	{
		commonFull(blackhole, "lz4bFull", (stream) -> {
			try {
				return new BlockLZ4CompressorOutputStream(stream);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Benchmark
	public void lz4fFull(Blackhole blackhole) throws IOException
	{
		commonFull(blackhole, "lz4fFull", (stream) -> {
			try {
				return new FramedLZ4CompressorOutputStream(stream);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Benchmark
	public void lz4fSplit(Blackhole blackhole) throws IOException
	{
		commonSplit(blackhole, "lz4fSplit", (stream) -> {
			try {
				return new FramedLZ4CompressorOutputStream(stream);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void commonFull(Blackhole blackhole, String name, Function<OutputStream, OutputStream> compressCreator) throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (OutputStream compressOutput = compressCreator.apply(output)) {
			compressOutput.write(FULL_INPUT);
		}
		if (!printedSize) {
			printedSize = true;
			log.error("{}, size={}", name, output.size());
		}
		blackhole.consume(output);

	}

	private void commonPart(Blackhole blackhole, String name, Function<OutputStream, OutputStream> compressCreator) throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (OutputStream compressOutput = compressCreator.apply(output)) {
			for (byte[] input: INPUTS) {
				compressOutput.write(input);
				compressOutput.flush();
			}
		}
		if (!printedSize) {
			printedSize = true;
			log.error("{}, size={}", name, output.size());
		}
		blackhole.consume(output);
	}

	private void commonSplit(Blackhole blackhole, String name, Function<OutputStream, OutputStream> compressCreator) throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		for (byte[] input: INPUTS) {
			try (OutputStream compressOutput = compressCreator.apply(output)) {
					compressOutput.write(input);
			}
		}
		if (!printedSize) {
			printedSize = true;
			log.error("{}, size={}", name, output.size());
		}
		blackhole.consume(output);
	}
}
