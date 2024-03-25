package cz.znj.kvr.sw.exp.java.benchmark.csv;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import cz.znj.kvr.sw.exp.java.benchmark.csv.support.TestDomain;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Log4j2
@State(Scope.Benchmark)
@Warmup(iterations = BenchmarkSettings.WARMUP_ITERATIONS)
@Measurement(iterations = BenchmarkSettings.MEASUREMENT_ITERATIONS, batchSize = BenchmarkSettings.BATCH_SIZE, time = BenchmarkSettings.TIMEOUT_SEC)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, jvmArgs = "-Xmx8G")
public class JacksonCsvBenchmark
{
	public static CsvMapper mapper = (CsvMapper) new CsvMapper()
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	public static CsvSchema schema = CsvSchema.emptySchema().withHeader().withoutColumns();
	public static CsvSchema columnSchema = CsvSchema.emptySchema().withHeader();

	@State(value = Scope.Benchmark)
	public static class ColumnState
	{
		InputStream input = InputGenerator.getInput();
		ObjectReader reader;
		MappingIterator<String[]> iterator;

		@SneakyThrows
		public ColumnState()
		{
			reader = mapper.readerFor(List.class).with(columnSchema);
			iterator = reader.readValues(input);
		}

		@TearDown
		public void teardown() throws IOException
		{
			input.close();
		}
	}

	@State(value = Scope.Benchmark)
	public static class MapState
	{
		InputStream input = InputGenerator.getInput();
		ObjectReader reader;
		MappingIterator<TestDomain> iterator;

		@SneakyThrows
		public MapState()
		{
			reader = mapper.readerFor(Map.class).with(schema);
			iterator = reader.readValues(input);
		}

		@TearDown
		public void teardown() throws IOException
		{
			input.close();
		}
	}

	@State(value = Scope.Benchmark)
	public static class KeyState
	{
		InputStream input = InputGenerator.getInput();
		ObjectReader reader;
		MappingIterator<TestDomain> iterator;

		@SneakyThrows
		public KeyState()
		{
			reader = mapper.readerFor(TestDomain.class).with(schema);
			iterator = reader.readValues(input);
		}

		@TearDown
		public void teardown() throws IOException
		{
			input.close();
		}
	}

//	@Benchmark
//	public void columnRead(ColumnState state, Blackhole blackhole) throws Exception
//	{
//		blackhole.consume(state.iterator.next());
//	}

	@Benchmark
	public void mapRead(KeyState state, Blackhole blackhole) throws Exception
	{
		blackhole.consume(state.iterator.next());
	}

	@Benchmark
	public void keyRead(KeyState state, Blackhole blackhole) throws Exception
	{
		blackhole.consume(state.iterator.next());
	}
}
