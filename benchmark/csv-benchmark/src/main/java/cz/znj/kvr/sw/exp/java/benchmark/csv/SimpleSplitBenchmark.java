package cz.znj.kvr.sw.exp.java.benchmark.csv;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;


@Log4j2
@State(Scope.Benchmark)
@Warmup(iterations = BenchmarkSettings.WARMUP_ITERATIONS)
@Measurement(iterations = BenchmarkSettings.MEASUREMENT_ITERATIONS, batchSize = BenchmarkSettings.BATCH_SIZE, time = BenchmarkSettings.TIMEOUT_SEC)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, jvmArgs = "-Xmx8G")
public class SimpleSplitBenchmark
{
	@State(value = Scope.Benchmark)
	public static class SplitReadState
	{
		InputStream input = InputGenerator.getInput();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		@TearDown
		public void teardown() throws IOException
		{
			input.close();
		}
	}

	@Benchmark
	public void columnRead(SplitReadState state, Blackhole blackhole) throws Exception
	{
		String line;
		line = state.reader.readLine();
		String[] values = line.split(",");
		blackhole.consume(new String[]{ values[0], values[9] });
	}
}
