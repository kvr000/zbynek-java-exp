package cz.znj.kvr.sw.exp.java.corejava.string.benchmark;

import lombok.extern.log4j.Log4j2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


/**
 * Benchmark for loading properties files from classpath.
 */
@Log4j2
@Fork(1)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class StringFormatVsConcatenationBenchmark
{
	public static final String HELLO = "Hello";
	public static final String JOIN = ", ";

	// One value goes separately, so too smart JVM does not optimize∞
	@Param({ "World" })
	public String value;

	@Benchmark
	public void benchmarkConcat(Blackhole blackhole) {
		blackhole.consume(HELLO+JOIN+value);
	}

	@Benchmark
	public void benchmarkFormatLocale(Blackhole blackhole) {
		blackhole.consume(String.format("%s, %s", HELLO, value));
	}

	@Benchmark
	public void benchmarkFormatRoot(Blackhole blackhole) {
		blackhole.consume(String.format(Locale.ROOT, "%s, %s", HELLO, value));
	}
}
