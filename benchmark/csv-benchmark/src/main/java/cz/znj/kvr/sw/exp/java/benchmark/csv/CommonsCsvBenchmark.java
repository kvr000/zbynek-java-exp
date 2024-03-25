package cz.znj.kvr.sw.exp.java.benchmark.csv;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;


@Log4j2
@State(Scope.Benchmark)
@Warmup(iterations = BenchmarkSettings.WARMUP_ITERATIONS)
@Measurement(iterations = BenchmarkSettings.MEASUREMENT_ITERATIONS, batchSize = BenchmarkSettings.BATCH_SIZE, time = BenchmarkSettings.TIMEOUT_SEC)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, jvmArgs = "-Xmx8G")
public class CommonsCsvBenchmark
{
	@State(value = Scope.Benchmark)
	public static class CsvReadState
	{
		InputStream input = InputGenerator.getInput();
		CSVParser reader;

		Iterator<CSVRecord> recordIterator;

		@SneakyThrows
		public CsvReadState()
		{
			reader = CSVFormat.DEFAULT.withHeader().parse(new InputStreamReader(input));
			recordIterator = reader.iterator();
		}

		@TearDown
		public void teardown() throws IOException
		{
			input.close();
		}
	}

	@Benchmark
	public void columnRead(CsvReadState state, Blackhole blackhole) throws Exception
	{
		CSVRecord record = state.recordIterator.next();
		blackhole.consume(new String[]{record.get(0), record.get(9)});
	}

	@Benchmark
	public void keyRead(CsvReadState state, Blackhole blackhole) throws Exception
	{
		CSVRecord record = state.recordIterator.next();
		blackhole.consume(new String[]{ record.get("col0"), record.get("col9") });
	}
}
