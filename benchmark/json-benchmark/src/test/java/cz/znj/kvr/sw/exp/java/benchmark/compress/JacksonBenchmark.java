package cz.znj.kvr.sw.exp.java.benchmark.compress;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.znj.kvr.sw.exp.java.benchmark.compress.support.TestDomain;
import lombok.extern.log4j.Log4j2;
import net.dryuf.bigio.iostream.LinedInputMultiStream;
import net.dryuf.bigio.iostream.MultiStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.Supplier;


@Log4j2
public class JacksonBenchmark
{
	public static ObjectMapper mapper = new ObjectMapper();

	public static byte[] json = (new Supplier<byte[]>() {
		@Override
		public byte[] get()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (int i = 0; i < 1_000_000; ++i) {
				sb.append("{\"firstname\":\"Zbynek\",\"surname\":\"Vyskovsky\"},");
			}
			sb.replace(sb.length()-1, sb.length(), "]");
			return sb.toString().getBytes(StandardCharsets.UTF_8);
		}
	}).get();

	@Benchmark
	@Fork(0)
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, time = 10)
	public void benchmarkBytesRead() throws IOException
	{
		mapper.readerFor(new TypeReference<Collection<TestDomain>>() {}).readValue(json);
	}

	@Benchmark
	@Fork(0)
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, time = 10)
	public void benchmarkStringRead() throws IOException
	{
		mapper.readerFor(new TypeReference<Collection<TestDomain>>() {}).readValue(new String(json));
	}

	@Benchmark
	@Fork(0)
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, time = 10)
	public void benchmarkStreamRead() throws IOException
	{
		mapper.readerFor(new TypeReference<Collection<TestDomain>>() {}).readValue(new ByteArrayInputStream(json));
	}

	@Benchmark
	@Fork(0)
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, time = 10)
	@Test
	public void benchmarkMultiStreamRead() throws IOException
	{
		try (MultiStream multiStream = new LinedInputMultiStream(new ByteArrayInputStream(json))) {
			try (InputStream stream = multiStream.nextStream()) {
				mapper.readerFor(new TypeReference<Collection<TestDomain>>() { }).readValue(stream);
			}
		}
	}
}
